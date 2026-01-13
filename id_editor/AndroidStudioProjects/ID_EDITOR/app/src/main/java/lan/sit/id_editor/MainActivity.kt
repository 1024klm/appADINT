package lan.sit.id_editor

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var scanner: AdintScanner
    private val isScanning = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanner = AdintScanner(this)
        runScan()
    }

    /**
     * Lance le scan en arrière-plan (avec protection contre les appels multiples)
     */
    private fun runScan() {
        // Empêcher les scans concurrents
        if (!isScanning.compareAndSet(false, true)) {
            return
        }

        showLoadingScreen()

        Thread {
            try {
                val result = scanner.scan()
                val deviceId = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.ANDROID_ID
                )

                // Vérifier que l'activité est encore valide
                if (!isFinishing && !isDestroyed) {
                    runOnUiThread {
                        showMainScreen(result, deviceId)
                    }
                }
            } finally {
                // Toujours libérer le verrou, même en cas d'exception
                isScanning.set(false)
            }
        }.start()
    }

    private fun showLoadingScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFF5F5F5.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        layout.addView(TextView(this).apply {
            text = "Analyse en cours..."
            textSize = 20f
            setTextColor(0xFF333333.toInt())
            gravity = Gravity.CENTER
        })

        layout.addView(TextView(this).apply {
            text = "Scan des signaux observables"
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        })

        setContentView(layout)
    }

    private fun showMainScreen(result: AdintResult, deviceId: String) {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(0xFFF5F5F5.toInt())
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        // === TITRE ===
        rootLayout.addView(TextView(this).apply {
            text = "Diagnostic ADINT"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF333333.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, 8)
        })

        rootLayout.addView(TextView(this).apply {
            text = "Audit de surface d'exposition publicitaire"
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, 32)
        })

        // === GESTION ERREUR ===
        if (!result.scanSuccessful) {
            rootLayout.addView(UIComponents.createErrorCard(
                this,
                "Erreur lors du scan",
                result.errorMessage ?: "Erreur inconnue"
            ))
        }

        // === SECTION 1: SCORE ===
        rootLayout.addView(UIComponents.createScoreCard(this, result, scanner))

        // === SECTION 2: SIGNAUX DÉTECTÉS ===
        rootLayout.addView(UIComponents.createProblemsCard(this, result.problems))

        // === SECTION 3: ACTIONS ===
        rootLayout.addView(UIComponents.createActionsCard(this, result))

        // === SECTION 4: RECOMMANDATIONS DE DURCISSEMENT ===
        rootLayout.addView(UIComponents.createRecommendationsCard(this))

        // === SECTION 5: LIMITES TECHNIQUES (bouton vers page séparée) ===
        rootLayout.addView(UIComponents.createLimitationsButton(this))

        // === SECTION 6: IDENTIFIANTS ===
        rootLayout.addView(UIComponents.createSectionTitle(this, "Vos identifiants"))

        // Device ID (masqué, avec bouton copier)
        rootLayout.addView(UIComponents.createIdentifierCard(
            context = this,
            label = "Android Device ID (ANDROID_ID)",
            displayValue = maskIdentifier(deviceId),
            description = "Identifiant stable, modifiable uniquement par réinitialisation d'usine. " +
                "Son usage pour l'ADINT est officiellement interdit par Google.",
            realValue = deviceId  // Pour le bouton Copier
        ))

        // Advertising ID (masqué, avec boutons Copier + Gérer)
        val gaidDisabled = result.gaid == null || result.gaid == "00000000-0000-0000-0000-000000000000"
        val displayGaid = when {
            result.gaid == null -> "Non disponible"
            gaidDisabled -> "ID désactivé"
            else -> maskIdentifier(result.gaid)
        }
        val latStatus = if (result.limitAdTrackingEnabled) {
            "LAT activé - vous avez demandé la limitation du suivi."
        } else {
            "LAT non activé - les apps peuvent utiliser cet ID pour le ciblage."
        }

        rootLayout.addView(UIComponents.createIdentifierCard(
            context = this,
            label = "Google Advertising ID (GAID)",
            displayValue = displayGaid,
            description = "$latStatus\n\nCet ID peut être réinitialisé ou supprimé dans les paramètres.",
            realValue = if (gaidDisabled) null else result.gaid,  // Pas de bouton Copier si désactivé
            showActionButton = true,
            actionButtonText = "Supprimer",
            actionHint = "Confidentialité → Annonces → Supprimer l'ID publicitaire",
            onActionClick = {
                // Essayer d'ouvrir directement les paramètres d'annonces Google
                try {
                    startActivity(Intent("com.google.android.gms.settings.ADS_PRIVACY"))
                } catch (e: Exception) {
                    // Fallback vers les paramètres de confidentialité
                    startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS))
                }
            }
        ))

        // UUID/Firebase (pas de bouton copier car non accessible)
        rootLayout.addView(UIComponents.createIdentifierCard(
            context = this,
            label = "UUID / Firebase Instance ID",
            displayValue = "Non accessible sans root",
            description = "Ces identifiants sont propres à chaque application et stockés dans " +
                "leurs données privées. Seul un accès root permet de les lire."
        ))

        // === BOUTON RELANCER ===
        rootLayout.addView(UIComponents.createRescanButton(this) {
            runScan()
        })

        scrollView.addView(rootLayout)
        setContentView(scrollView)
    }

    /**
     * Masque un identifiant pour la privacy (affiche premiers et derniers caractères)
     * Ex: "abc12345-wxyz" -> "abc1...wxyz"
     */
    private fun maskIdentifier(id: String): String {
        if (id.length <= 8) return id
        val start = id.take(4)
        val end = id.takeLast(4)
        return "$start...$end"
    }
}
