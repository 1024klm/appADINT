package lan.sit.id_editor

import android.graphics.Typeface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import android.content.Intent
import android.widget.Button


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Récupération du device ID
        val deviceId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )

        // Création d'un thread pour récupérer l' AD ID
        fun fetchAdId(callback: (adId: String?, limitTracking: Boolean) -> Unit) {
            Thread(Runnable {
                try {
                    val adInfo: AdvertisingIdClient.Info = AdvertisingIdClient.getAdvertisingIdInfo(this)

                    val adId: String? = adInfo.getId()
                    val limitTracking: Boolean = adInfo.isLimitAdTrackingEnabled() // Signifie que les apps peuvent utiliser l'ID pour créer des pubs personnalisées
                    callback(adId, limitTracking) // Récupération de la valeur sur le thread principal
                    Log.d("AD_ID", "Advertising ID: " + adId)
                    Log.d("AD_ID", "Limit Ad Tracking: " + limitTracking)
                } catch (e: Exception) {
                    val msg = "Failed to get Ad ID"
                    Log.e("AD_ID", msg, e)
                }
            }).start()
        }

        fetchAdId { adId, limitTracking ->
            runOnUiThread {
                val infoList = listOf(
                    listOf(
                        "Android Device ID",
                        deviceId,
                        "Cet identifiant n'est modifiable qu'à la réinitialisation d'usine de l'appareil. Son usage pour l'ADINT est explicitement interdit."
                    ),
                    listOf(
                        "Advertising ID",
                        adId,
                        "Il est possible de supprimer cet identifiant depuis les paramètres de confidentialité (Aller dans autres paramètres > Annonces)\n\nLe limit Tracking est fixé à $limitTracking. Cela signifie que les applications peuvent s'en servir pour créer des pubs personnalisées."
                    ),
                            listOf(
                            "UUID/Firebase ID/GUID",
                    "null",
                    "Identifiants propres à chaque application. Accessibles uniquement sur un téléphone rooté"
                )
                )

                val rootLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(48, 48, 48, 48)
                    setBackgroundColor(0xFFF5F5F5.toInt())
                }

                // Titre de la page principale
                val titleView = TextView(this).apply {
                    text = "Identifiants android"
                    textSize = 22f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(0xFF333333.toInt())
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(0, 0, 0, 48)
                }

                rootLayout.addView(titleView)

                // Création d'une bulle pour chaque ID de la liste
                infoList.forEach { (label, value, desc) ->
                    rootLayout.addView(createInfoItem(label, value, desc))
                }

                setContentView(rootLayout)
            }
        }
    }
    private fun createInfoItem(
        label: String,
        value: String,
        desc: String
    ): LinearLayout {
        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(0xFFFFFFFF.toInt())
            setStroke(2, 0xFFDDDDDD.toInt())
            cornerRadius = 24f
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            background = backgroundDrawable


            // Space between items
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 48

            }

            val labelView = TextView(context).apply {
                text = label
                textSize = 14f
                setTextColor(0xFF777777.toInt())
            }

            val valueView = TextView(context).apply {
                text = value
                textSize = 16f
                setTypeface(Typeface.MONOSPACE)
                setTextColor(0xFF000000.toInt())
                setPadding(0, 8, 0, 16)
            }

            val descView = TextView(context).apply {
                text = desc
                textSize = 14f
                setPadding(4,4,4,12)
                setTextColor(0xFF777777.toInt())
            }

            addView(labelView)
            addView(valueView)
            addView(descView)

            // Ajout du bouton de suppression (même si on n'a pas les droits)
            if (label == "Advertising ID") {
                val resetButton = Button(context).apply {
                    text = "Supprimer l'Advertising ID"
                    setBackgroundColor(Color.RED)
                    setOnClickListener {
                        startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                    }
                }
                addView(resetButton)
            }
        }
    }

}
