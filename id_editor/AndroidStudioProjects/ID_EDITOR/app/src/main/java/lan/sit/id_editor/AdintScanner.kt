package lan.sit.id_editor

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.ads.identifier.AdvertisingIdClient

/**
 * Résultat du scan ADINT
 */
data class AdintResult(
    val score: Int,                    // Score sur 10
    val gaid: String?,                 // Google Advertising ID
    val limitTracking: Boolean,        // Si le tracking est limité
    val appsWithLocationAndInternet: Int,  // Nombre d'apps à risque
    val appsWithManyPermissions: Int,  // Apps avec >10 permissions
    val problems: List<Problem>        // Liste des problèmes détectés
)

data class Problem(
    val severity: Severity,  // CRITICAL, WARNING, INFO
    val title: String,
    val description: String
)

enum class Severity { CRITICAL, WARNING, INFO }

enum class RiskLevel(val label: String, val color: Int) {
    LOW("Faible", 0xFF4CAF50.toInt()),      // Vert
    MEDIUM("Moyen", 0xFFFF9800.toInt()),    // Orange
    HIGH("Élevé", 0xFFF44336.toInt())       // Rouge
}

/**
 * Scanner ADINT - Analyse l'exposition du device
 */
class AdintScanner(private val context: Context) {

    companion object {
        // Points pour le calcul du score
        const val POINTS_PERSONALIZED_ADS = 3
        const val POINTS_GAID_ACCESSIBLE = 2
        const val POINTS_PER_RISKY_APP = 1
        const val MAX_RISKY_APP_POINTS = 3
        const val POINTS_PER_PERMISSION_HEAVY_APP = 1
        const val MAX_PERMISSION_HEAVY_POINTS = 2
        const val PERMISSION_THRESHOLD = 10
    }

    /**
     * Exécute le scan complet (doit être appelé depuis un thread background)
     */
    fun scan(): AdintResult {
        // Récupérer les infos publicitaires
        val (gaid, limitTracking) = getAdvertisingInfo()

        // Scanner les applications
        val (locationApps, heavyPermissionApps) = scanInstalledApps()

        // Construire la liste des problèmes
        val problems = mutableListOf<Problem>()

        // Problème 1: Pub personnalisée activée
        if (!limitTracking) {
            problems.add(Problem(
                Severity.CRITICAL,
                "Annonces personnalisées activées",
                "Les applications peuvent utiliser votre ID publicitaire pour créer un profil de vos habitudes."
            ))
        }

        // Problème 2: GAID accessible
        if (gaid != null && gaid != "00000000-0000-0000-0000-000000000000") {
            problems.add(Problem(
                Severity.CRITICAL,
                "ID publicitaire accessible",
                "Votre identifiant publicitaire ($gaid) peut être utilisé pour vous suivre entre les applications."
            ))
        }

        // Problème 3: Apps avec localisation + internet
        if (locationApps > 0) {
            problems.add(Problem(
                Severity.WARNING,
                "$locationApps app(s) avec localisation + internet",
                "Ces applications peuvent potentiellement partager votre position avec des tiers."
            ))
        }

        // Problème 4: Apps avec beaucoup de permissions
        if (heavyPermissionApps > 0) {
            problems.add(Problem(
                Severity.WARNING,
                "$heavyPermissionApps app(s) avec nombreuses permissions",
                "Ces applications ont accès à plus de $PERMISSION_THRESHOLD permissions, ce qui augmente la surface d'attaque."
            ))
        }

        // Calculer le score
        val score = calculateScore(limitTracking, gaid, locationApps, heavyPermissionApps)

        return AdintResult(
            score = score,
            gaid = gaid,
            limitTracking = limitTracking,
            appsWithLocationAndInternet = locationApps,
            appsWithManyPermissions = heavyPermissionApps,
            problems = problems
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
            Pair(null, true) // En cas d'erreur, on considère le tracking limité
        }
    }

    /**
     * Scanne les applications installées pour trouver celles à risque
     */
    private fun scanInstalledApps(): Pair<Int, Int> {
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

            for (packageInfo in packages) {
                val permissions = packageInfo.requestedPermissions ?: continue

                // Vérifier si l'app a localisation + internet
                val hasLocation = permissions.any {
                    it == Manifest.permission.ACCESS_FINE_LOCATION ||
                    it == Manifest.permission.ACCESS_COARSE_LOCATION
                }
                val hasInternet = permissions.contains(Manifest.permission.INTERNET)

                if (hasLocation && hasInternet) {
                    // Exclure notre propre app et les apps système
                    if (packageInfo.packageName != context.packageName &&
                        !isSystemApp(packageInfo)) {
                        locationAndInternetCount++
                    }
                }

                // Vérifier si l'app a beaucoup de permissions
                if (permissions.size > PERMISSION_THRESHOLD && !isSystemApp(packageInfo)) {
                    heavyPermissionCount++
                }
            }
        } catch (e: Exception) {
            // Permission QUERY_ALL_PACKAGES peut être refusée
        }

        return Pair(locationAndInternetCount, heavyPermissionCount)
    }

    /**
     * Vérifie si une app est une app système
     */
    private fun isSystemApp(packageInfo: PackageInfo): Boolean {
        return (packageInfo.applicationInfo?.flags ?: 0) and
               android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
    }

    /**
     * Calcule le score d'exposition ADINT (0-10)
     */
    private fun calculateScore(
        limitTracking: Boolean,
        gaid: String?,
        locationApps: Int,
        heavyPermissionApps: Int
    ): Int {
        var score = 0

        // +3 si pub personnalisée activée
        if (!limitTracking) {
            score += POINTS_PERSONALIZED_ADS
        }

        // +2 si GAID accessible
        if (gaid != null && gaid != "00000000-0000-0000-0000-000000000000") {
            score += POINTS_GAID_ACCESSIBLE
        }

        // +1 par app à risque (max 3)
        score += minOf(locationApps, MAX_RISKY_APP_POINTS) * POINTS_PER_RISKY_APP

        // +1 par app avec beaucoup de permissions (max 2)
        score += minOf(heavyPermissionApps, MAX_PERMISSION_HEAVY_POINTS) * POINTS_PER_PERMISSION_HEAVY_APP

        return minOf(score, 10)
    }

    /**
     * Détermine le niveau de risque basé sur le score
     */
    fun getRiskLevel(score: Int): RiskLevel {
        return when {
            score <= 2 -> RiskLevel.LOW
            score <= 5 -> RiskLevel.MEDIUM
            else -> RiskLevel.HIGH
        }
    }
}
