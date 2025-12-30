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

class MainActivity : AppCompatActivity() {

    private lateinit var scanner: AdintScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanner = AdintScanner(this)
        runScan()
    }

    /**
     * Lance le scan en arrière-plan
     */
    private fun runScan() {
        showLoadingScreen()

        Thread {
            val result = scanner.scan()
            val deviceId = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            )

            runOnUiThread {
                showMainScreen(result, deviceId)
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
            rootLayout.addView(UIComponents.createCard(this).apply {
                setBackgroundColor(0xFFFFCDD2.toInt())
                addView(TextView(context).apply {
                    text = "Erreur lors du scan"
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(0xFFC62828.toInt())
                })
                addView(TextView(context).apply {
                    text = result.errorMessage ?: "Erreur inconnue"
                    textSize = 14f
                    setTextColor(0xFFD32F2F.toInt())
                })
            })
        }

        // === SECTION 1: SCORE ===
        rootLayout.addView(UIComponents.createScoreCard(this, result, scanner))

        // === SECTION 2: SIGNAUX DÉTECTÉS ===
        rootLayout.addView(UIComponents.createProblemsCard(this, result.problems))

        // === SECTION 3: ACTIONS ===
        rootLayout.addView(UIComponents.createActionsCard(this, result))

        // === SECTION 4: LIMITES TECHNIQUES ===
        rootLayout.addView(UIComponents.createLimitationsCard(this))

        // === SECTION 5: IDENTIFIANTS ===
        rootLayout.addView(UIComponents.createSectionTitle(this, "Vos identifiants"))

        // Device ID
        rootLayout.addView(UIComponents.createIdentifierCard(
            this,
            "Android Device ID (ANDROID_ID)",
            deviceId,
            "Identifiant stable, modifiable uniquement par réinitialisation d'usine. " +
            "Son usage pour l'ADINT est officiellement interdit par Google."
        ))

        // Advertising ID
        val latStatus = if (result.limitAdTrackingEnabled) {
            "LAT activé - vous avez demandé la limitation du suivi."
        } else {
            "LAT non activé - les apps peuvent utiliser cet ID pour le ciblage."
        }

        rootLayout.addView(UIComponents.createIdentifierCard(
            this,
            "Google Advertising ID (GAID)",
            result.gaid ?: "Non disponible (Play Services absent ?)",
            "$latStatus\n\nCet ID peut être réinitialisé ou supprimé dans les paramètres.",
            showResetButton = true,
            onResetClick = {
                startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS))
            }
        ))

        // UUID/Firebase
        rootLayout.addView(UIComponents.createIdentifierCard(
            this,
            "UUID / Firebase Instance ID",
            "Non accessible sans root",
            "Ces identifiants sont propres à chaque application et stockés dans " +
            "leurs données privées. Seul un accès root permet de les lire."
        ))

        // === BOUTON RELANCER ===
        rootLayout.addView(UIComponents.createRescanButton(this) {
            runScan()
        })

        scrollView.addView(rootLayout)
        setContentView(scrollView)
    }
}
