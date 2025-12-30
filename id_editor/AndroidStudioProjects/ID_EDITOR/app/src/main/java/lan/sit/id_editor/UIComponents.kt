package lan.sit.id_editor

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Composants UI réutilisables pour l'application ADINT
 */
object UIComponents {

    /**
     * Crée une carte avec fond blanc et coins arrondis
     */
    fun createCard(context: Context): LinearLayout {
        val background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(0xFFFFFFFF.toInt())
            setStroke(2, 0xFFDDDDDD.toInt())
            cornerRadius = 24f
        }

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            this.background = background
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }
    }

    /**
     * Crée la carte du score d'exposition ADINT
     */
    fun createScoreCard(context: Context, result: AdintResult, scanner: AdintScanner): LinearLayout {
        val riskLevel = scanner.getRiskLevel(result.score)

        return createCard(context).apply {
            // Titre avec mention "heuristique"
            addView(TextView(context).apply {
                text = "SCORE D'EXPOSITION (heuristique)"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF333333.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 8)
            })

            // Sous-titre explicatif
            addView(TextView(context).apply {
                text = "Basé sur des signaux observables"
                textSize = 11f
                setTextColor(0xFF888888.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            })

            // Score grand format
            addView(TextView(context).apply {
                text = "${result.score}/10"
                textSize = 48f
                setTypeface(null, Typeface.BOLD)
                setTextColor(riskLevel.color)
                gravity = Gravity.CENTER
            })

            // Niveau de risque
            addView(TextView(context).apply {
                text = riskLevel.label.uppercase()
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(riskLevel.color)
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 24)
            })

            // Barre de progression
            addView(createScoreBar(context, result.score, riskLevel.color))

            // Détails du calcul
            addView(TextView(context).apply {
                text = "Composition du score :"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF555555.toInt())
                setPadding(0, 24, 0, 8)
            })

            // Liste des points (wording corrigé)
            val details = mutableListOf<String>()
            if (!result.limitAdTrackingEnabled) {
                details.add("• LAT non activé : +${AdintScanner.POINTS_LAT_DISABLED}")
            }
            val gaidValid = result.gaid != null && result.gaid != "00000000-0000-0000-0000-000000000000"
            if (gaidValid) {
                details.add("• GAID accessible : +${AdintScanner.POINTS_GAID_ACCESSIBLE}")
            }
            if (result.appsWithLocationAndInternet > 0) {
                val points = minOf(result.appsWithLocationAndInternet, AdintScanner.MAX_RISKY_APP_POINTS)
                details.add("• ${result.appsWithLocationAndInternet} app(s) loc+net : +$points")
            }
            if (result.appsWithManyPermissions > 0) {
                val points = minOf(result.appsWithManyPermissions, AdintScanner.MAX_PERMISSION_HEAVY_POINTS)
                details.add("• ${result.appsWithManyPermissions} app(s) >10 perms : +$points")
            }

            if (details.isEmpty()) {
                details.add("• Aucun signal de risque détecté")
            }

            addView(TextView(context).apply {
                text = details.joinToString("\n")
                textSize = 13f
                setTextColor(0xFF666666.toInt())
            })
        }
    }

    /**
     * Crée une barre de progression pour le score
     */
    private fun createScoreBar(context: Context, score: Int, color: Int): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)

            val barBackground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xFFE0E0E0.toInt())
                cornerRadius = 12f
            }

            val barContainer = LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(600, 24)
                background = barBackground
            }

            val fillWidth = (600 * score / 10)
            val barFill = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(fillWidth, LinearLayout.LayoutParams.MATCH_PARENT)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(color)
                    cornerRadius = 12f
                }
            }

            barContainer.addView(barFill)
            addView(barContainer)
        }
    }

    /**
     * Crée la carte des signaux détectés (pas "problèmes")
     */
    fun createProblemsCard(context: Context, problems: List<Problem>): LinearLayout {
        return createCard(context).apply {
            addView(TextView(context).apply {
                text = "SIGNAUX DÉTECTÉS (${problems.size})"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF333333.toInt())
                setPadding(0, 0, 0, 16)
            })

            if (problems.isEmpty()) {
                addView(TextView(context).apply {
                    text = "Aucun signal d'alerte"
                    textSize = 14f
                    setTextColor(0xFF4CAF50.toInt())
                })
            } else {
                problems.forEach { problem ->
                    addView(createProblemItem(context, problem))
                }
            }
        }
    }

    private fun createProblemItem(context: Context, problem: Problem): LinearLayout {
        val iconColor = when (problem.severity) {
            Severity.CRITICAL -> 0xFFF44336.toInt()
            Severity.WARNING -> 0xFFFF9800.toInt()
            Severity.INFO -> 0xFF2196F3.toInt()
        }

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)

            addView(TextView(context).apply {
                text = "●"
                textSize = 16f
                setTextColor(iconColor)
                setPadding(0, 0, 16, 0)
            })

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                addView(TextView(context).apply {
                    text = problem.title
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(0xFF333333.toInt())
                })

                addView(TextView(context).apply {
                    text = problem.description
                    textSize = 12f
                    setTextColor(0xFF666666.toInt())
                })
            })
        }
    }

    /**
     * Crée la carte des actions recommandées
     */
    fun createActionsCard(context: Context, result: AdintResult): LinearLayout {
        return createCard(context).apply {
            addView(TextView(context).apply {
                text = "ACTIONS POSSIBLES"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF333333.toInt())
                setPadding(0, 0, 0, 16)
            })

            // Gérer ID pub
            addView(createActionButton(
                context,
                "Gérer l'ID publicitaire",
                "Réinitialiser ou limiter le suivi",
                Settings.ACTION_PRIVACY_SETTINGS
            ))

            // Permissions des apps
            addView(createActionButton(
                context,
                "Permissions des applications",
                "Vérifier les accès accordés",
                Settings.ACTION_APPLICATION_SETTINGS
            ))

            // Localisation
            addView(createActionButton(
                context,
                "Paramètres de localisation",
                "Contrôler l'accès à votre position",
                Settings.ACTION_LOCATION_SOURCE_SETTINGS
            ))

            // DNS privé (Android 9+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                addView(createActionButton(
                    context,
                    "Réseau / DNS privé",
                    "Améliorer la confidentialité réseau",
                    Settings.ACTION_WIRELESS_SETTINGS
                ))
            }
        }
    }

    private fun createActionButton(
        context: Context,
        title: String,
        subtitle: String,
        settingsAction: String
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 12)
            isClickable = true
            isFocusable = true

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                addView(TextView(context).apply {
                    text = title
                    textSize = 14f
                    setTextColor(0xFF333333.toInt())
                })

                addView(TextView(context).apply {
                    text = subtitle
                    textSize = 12f
                    setTextColor(0xFF888888.toInt())
                })
            })

            addView(Button(context).apply {
                text = "→"
                textSize = 18f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(120, 80)

                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(0xFF2196F3.toInt())
                    cornerRadius = 12f
                }

                setOnClickListener {
                    try {
                        context.startActivity(Intent(settingsAction))
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            })
        }
    }

    /**
     * Crée la carte des LIMITES TECHNIQUES
     */
    fun createLimitationsCard(context: Context): LinearLayout {
        return createCard(context).apply {
            // Fond légèrement différent pour distinguer
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xFFFFF8E1.toInt()) // Jaune pâle
                setStroke(2, 0xFFFFE082.toInt())
                cornerRadius = 24f
            }

            addView(TextView(context).apply {
                text = "LIMITES DE CE DIAGNOSTIC"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF6D4C41.toInt())
                setPadding(0, 0, 0, 12)
            })

            val limitations = listOf(
                "• Ne détecte PAS les SDK publicitaires intégrés",
                "• Ne surveille PAS le trafic réseau réel",
                "• Ne détecte PAS le fingerprinting (canvas, audio...)",
                "• Le flag LAT est déclaratif, pas une garantie",
                "• Les permissions déclarées ≠ permissions utilisées",
                "• Prototype de laboratoire, non destiné au Play Store"
            )

            addView(TextView(context).apply {
                text = limitations.joinToString("\n")
                textSize = 12f
                setTextColor(0xFF5D4037.toInt())
                setLineSpacing(4f, 1f)
            })
        }
    }

    /**
     * Crée une carte d'identifiant
     */
    fun createIdentifierCard(
        context: Context,
        label: String,
        value: String?,
        description: String,
        showResetButton: Boolean = false,
        onResetClick: (() -> Unit)? = null
    ): LinearLayout {
        return createCard(context).apply {
            addView(TextView(context).apply {
                text = label
                textSize = 14f
                setTextColor(0xFF777777.toInt())
            })

            addView(TextView(context).apply {
                text = value ?: "Non disponible"
                textSize = 16f
                setTypeface(Typeface.MONOSPACE)
                setTextColor(0xFF000000.toInt())
                setPadding(0, 8, 0, 16)
            })

            addView(TextView(context).apply {
                text = description
                textSize = 14f
                setPadding(4, 4, 4, 12)
                setTextColor(0xFF777777.toInt())
            })

            if (showResetButton && onResetClick != null) {
                addView(Button(context).apply {
                    text = "Gérer cet identifiant"
                    setBackgroundColor(0xFF2196F3.toInt())
                    setTextColor(Color.WHITE)
                    setOnClickListener { onResetClick() }
                })
            }
        }
    }

    /**
     * Crée un titre de section
     */
    fun createSectionTitle(context: Context, title: String): TextView {
        return TextView(context).apply {
            text = title
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF333333.toInt())
            setPadding(0, 32, 0, 16)
        }
    }

    /**
     * Crée un bouton de relance du scan
     */
    fun createRescanButton(context: Context, onClick: () -> Unit): Button {
        return Button(context).apply {
            text = "Relancer l'analyse"
            textSize = 14f
            setTextColor(Color.WHITE)

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xFF607D8B.toInt())
                cornerRadius = 16f
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 24
                bottomMargin = 48
            }

            setOnClickListener { onClick() }
        }
    }
}
