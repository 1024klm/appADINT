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

        // Afficher un écran de chargement
        showLoadingScreen()

        // Lancer le scan en arrière-plan
        Thread {
            val result = scanner.scan()

            // Récupérer le Device ID
            val deviceId = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            )

            runOnUiThread {
                showMainScreen(result, deviceId)
            }
        }.start()
    }

    /**
     * Affiche l'écran de chargement pendant le scan
     */
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
            text = "Scan des applications et paramètres"
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        })

        setContentView(layout)
    }

    /**
     * Affiche l'écran principal avec les résultats
     */
    private fun showMainScreen(result: AdintResult, deviceId: String) {
        // ScrollView pour permettre le défilement
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(0xFFF5F5F5.toInt())
        }

        // Layout principal
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        // === TITRE PRINCIPAL ===
        rootLayout.addView(TextView(this).apply {
            text = "Diagnostic ADINT"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF333333.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, 32)
        })

        // === SECTION 1: SCORE D'EXPOSITION ===
        rootLayout.addView(UIComponents.createScoreCard(this, result, scanner))

        // === SECTION 2: PROBLÈMES DÉTECTÉS ===
        rootLayout.addView(UIComponents.createProblemsCard(this, result.problems))

        // === SECTION 3: ACTIONS RECOMMANDÉES ===
        rootLayout.addView(UIComponents.createActionsCard(this, result))

        // === SECTION 4: IDENTIFIANTS ===
        rootLayout.addView(UIComponents.createSectionTitle(this, "Vos identifiants"))

        // Device ID
        rootLayout.addView(UIComponents.createIdentifierCard(
            this,
            "Android Device ID",
            deviceId,
            "Cet identifiant n'est modifiable qu'à la réinitialisation d'usine de l'appareil. Son usage pour l'ADINT est explicitement interdit."
        ))

        // Advertising ID
        val adIdDescription = buildString {
            append("Il est possible de supprimer cet identifiant depuis les paramètres de confidentialité.\n\n")
            if (result.limitTracking) {
                append("Le suivi publicitaire est limité.")
            } else {
                append("Le suivi publicitaire est activé - les applications peuvent créer des pubs personnalisées.")
            }
        }

        rootLayout.addView(UIComponents.createIdentifierCard(
            this,
            "Advertising ID",
            result.gaid ?: "Non disponible",
            adIdDescription,
            showResetButton = true,
            onResetClick = {
                startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS))
            }
        ))

        // UUID/Firebase
        rootLayout.addView(UIComponents.createIdentifierCard(
            this,
            "UUID/Firebase ID/GUID",
            "Non accessible",
            "Identifiants propres à chaque application. Accessibles uniquement sur un téléphone rooté."
        ))

        // Ajouter le layout au ScrollView
        scrollView.addView(rootLayout)
        setContentView(scrollView)
    }
}
