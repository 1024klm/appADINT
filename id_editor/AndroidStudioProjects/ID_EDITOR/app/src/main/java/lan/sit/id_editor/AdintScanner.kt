package lan.sit.id_editor

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.ads.identifier.AdvertisingIdClient

/**
 * Résultat du scan ADINT
 *
 * IMPORTANT: Ce scan est une HEURISTIQUE basée sur des signaux observables.
 * Il ne détecte PAS : les SDK publicitaires, le fingerprinting, les destinations réseau,
 * ni le tracking réel effectué par les applications.
 */
data class AdintResult(
    val score: Int,                        // Score de durcissement (10=bien protégé, 0=non protégé)
    val gaid: String?,                     // Google Advertising ID
    val limitAdTrackingEnabled: Boolean,   // Flag LAT (Limit Ad Tracking) de Play Services
    val appsWithLocationAndInternet: Int,  // Nombre d'apps avec ces permissions
    val appsWithManyPermissions: Int,      // Apps avec >10 permissions
    val problems: List<Problem>,           // Signaux d'alerte détectés
    val scanSuccessful: Boolean = true,    // Si le scan a pu s'exécuter
    val errorMessage: String? = null       // Message d'erreur éventuel
)

data class Problem(
    val severity: Severity,
    val title: String,
    val description: String
)

enum class Severity { CRITICAL, WARNING, INFO }

enum class ProtectionLevel(val label: String, val color: Int) {
    HIGH("Élevé", 0xFF4CAF50.toInt()),      // Vert = bien protégé
    MEDIUM("Moyen", 0xFFFF9800.toInt()),
    LOW("Faible", 0xFFF44336.toInt())        // Rouge = peu protégé
}

/**
 * Scanner ADINT - Analyse la surface d'exposition publicitaire
 *
 * LIMITES TECHNIQUES :
 * - Ne détecte pas les SDK publicitaires intégrés aux apps
 * - Ne détecte pas le fingerprinting (canvas, audio, etc.)
 * - Ne surveille pas le trafic réseau
 * - Le flag LAT n'est qu'une déclaration, pas une garantie
 * - Le scan des apps nécessite QUERY_ALL_PACKAGES (Android 11+)
 */
class AdintScanner(private val context: Context) {

    companion object {
        // Points pour le calcul du score (HEURISTIQUE)
        const val POINTS_LAT_DISABLED = 3       // LAT non activé
        const val POINTS_GAID_ACCESSIBLE = 2    // GAID récupérable
        const val POINTS_PER_RISKY_APP = 1
        const val MAX_RISKY_APP_POINTS = 3
        const val POINTS_PER_PERMISSION_HEAVY_APP = 1
        const val MAX_PERMISSION_HEAVY_POINTS = 2
        const val PERMISSION_THRESHOLD = 10
    }

    /**
     * Exécute le scan (doit être appelé depuis un thread background)
     */
    fun scan(): AdintResult {
        return try {
            performScan()
        } catch (e: Exception) {
            // Retourner un résultat d'erreur plutôt que crasher
            AdintResult(
                score = 0,
                gaid = null,
                limitAdTrackingEnabled = true,
                appsWithLocationAndInternet = 0,
                appsWithManyPermissions = 0,
                problems = emptyList(),
                scanSuccessful = false,
                errorMessage = "Erreur lors du scan : ${e.message}"
            )
        }
    }

    private fun performScan(): AdintResult {
        // Récupérer les infos publicitaires
        val (gaid, latEnabled) = getAdvertisingInfo()

        // Scanner les applications (peut échouer sans QUERY_ALL_PACKAGES)
        val (locationApps, heavyPermissionApps, appScanSuccessful) = scanInstalledApps()

        // Construire la liste des signaux d'alerte
        val problems = mutableListOf<Problem>()

        // Signal 1: LAT non activé
        // IMPORTANT: On ne dit PAS "pub personnalisée activée" car ce n'est pas vérifiable
        if (!latEnabled) {
            problems.add(Problem(
                Severity.CRITICAL,
                "Limitation du suivi (LAT) non activée",
                "Le flag 'Limit Ad Tracking' n'est pas activé dans les paramètres Google. " +
                "Cela signifie que les apps PEUVENT utiliser votre ID pub, mais pas qu'elles le font."
            ))
        }

        // Signal 2: GAID accessible (non null et non zeros)
        val gaidIsValid = gaid != null && gaid != "00000000-0000-0000-0000-000000000000"
        if (gaidIsValid) {
            problems.add(Problem(
                Severity.WARNING,
                "ID publicitaire (GAID) accessible",
                "Votre GAID est : $gaid. Cet identifiant peut être lu par les apps pour vous suivre entre applications."
            ))
        }

        // Signal 3: Apps avec localisation + internet (si scan réussi)
        if (appScanSuccessful && locationApps > 0) {
            problems.add(Problem(
                Severity.WARNING,
                "$locationApps app(s) : localisation + internet",
                "Ces apps DÉCLARENT ces permissions. Cela ne prouve pas qu'elles envoient votre position à des tiers."
            ))
        }

        // Signal 4: Apps avec beaucoup de permissions
        if (appScanSuccessful && heavyPermissionApps > 0) {
            problems.add(Problem(
                Severity.INFO,
                "$heavyPermissionApps app(s) avec >$PERMISSION_THRESHOLD permissions",
                "Un grand nombre de permissions augmente la surface d'attaque potentielle."
            ))
        }

        // Avertissement si scan apps a échoué
        if (!appScanSuccessful) {
            problems.add(Problem(
                Severity.INFO,
                "Scan des applications non disponible",
                "Android ${Build.VERSION.SDK_INT} restreint l'accès à la liste des apps installées."
            ))
        }

        // Calculer le score heuristique
        val score = calculateScore(latEnabled, gaidIsValid, locationApps, heavyPermissionApps)

        return AdintResult(
            score = score,
            gaid = gaid,
            limitAdTrackingEnabled = latEnabled,
            appsWithLocationAndInternet = locationApps,
            appsWithManyPermissions = heavyPermissionApps,
            problems = problems,
            scanSuccessful = true
        )
    }

    /**
     * Récupère les informations publicitaires Google
     */
    private fun getAdvertisingInfo(): Pair<String?, Boolean> {
        return try {
            val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            Pair(adInfo.id, adInfo.isLimitAdTrackingEnabled)
        } catch (e: Exception) {
            // Play Services absent ou erreur
            Pair(null, true)
        }
    }

    /**
     * Scanne les applications installées
     * Retourne (locationApps, heavyPermissionApps, success)
     *
     * Note: Nécessite QUERY_ALL_PACKAGES sur Android 11+ pour une visibilité complète.
     * Sans cette permission, seules les apps "visibles" seront listées.
     */
    private fun scanInstalledApps(): Triple<Int, Int, Boolean> {
        var locationAndInternetCount = 0
        var heavyPermissionCount = 0

        try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getInstalledPackages(
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }

            // Liste vide = échec probable (Play Services absent, restriction OEM, etc.)
            if (packages.isEmpty()) {
                return Triple(0, 0, false)
            }

            for (packageInfo in packages) {
                val permissions = packageInfo.requestedPermissions ?: continue

                // Vérifier localisation + internet
                val hasLocation = permissions.any {
                    it == Manifest.permission.ACCESS_FINE_LOCATION ||
                    it == Manifest.permission.ACCESS_COARSE_LOCATION
                }
                val hasInternet = permissions.contains(Manifest.permission.INTERNET)

                if (hasLocation && hasInternet) {
                    if (packageInfo.packageName != context.packageName && !isSystemApp(packageInfo)) {
                        locationAndInternetCount++
                    }
                }

                // Vérifier nombre de permissions
                if (permissions.size > PERMISSION_THRESHOLD && !isSystemApp(packageInfo)) {
                    heavyPermissionCount++
                }
            }

            // Succès même si peu d'apps (peut être un device clean ou work profile)
            return Triple(locationAndInternetCount, heavyPermissionCount, true)

        } catch (e: Exception) {
            // SecurityException, etc.
            return Triple(0, 0, false)
        }
    }

    private fun isSystemApp(packageInfo: PackageInfo): Boolean {
        return (packageInfo.applicationInfo?.flags ?: 0) and
               android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
    }

    /**
     * Calcule le score de durcissement (0-10)
     * 10 = téléphone bien durci, 0 = téléphone non durci
     *
     * ATTENTION : Ce score est INDICATIF et non scientifique.
     */
    private fun calculateScore(
        latEnabled: Boolean,
        gaidAccessible: Boolean,
        locationApps: Int,
        heavyPermissionApps: Int
    ): Int {
        var penalties = 0

        if (!latEnabled) penalties += POINTS_LAT_DISABLED
        if (gaidAccessible) penalties += POINTS_GAID_ACCESSIBLE
        penalties += minOf(locationApps, MAX_RISKY_APP_POINTS) * POINTS_PER_RISKY_APP
        penalties += minOf(heavyPermissionApps, MAX_PERMISSION_HEAVY_POINTS) * POINTS_PER_PERMISSION_HEAVY_APP

        return maxOf(10 - penalties, 0)
    }

    fun getProtectionLevel(score: Int): ProtectionLevel {
        return when {
            score >= 8 -> ProtectionLevel.HIGH
            score >= 5 -> ProtectionLevel.MEDIUM
            else -> ProtectionLevel.LOW
        }
    }
}
