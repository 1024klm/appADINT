package lan.sit.id_editor

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
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
            // Titre
            addView(TextView(context).apply {
                text = "SCORE D'EXPOSITION ADINT"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF333333.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
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
                text = "Détails du calcul :"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF555555.toInt())
                setPadding(0, 24, 0, 8)
            })

            // Liste des points
            val details = mutableListOf<String>()
            if (!result.limitTracking) {
                details.add("• Pub personnalisée activée : +${AdintScanner.POINTS_PERSONALIZED_ADS}")
            }
            if (result.gaid != null && result.gaid != "00000000-0000-0000-0000-000000000000") {
                details.add("• ID publicitaire accessible : +${AdintScanner.POINTS_GAID_ACCESSIBLE}")
            }
            if (result.appsWithLocationAndInternet > 0) {
                val points = minOf(result.appsWithLocationAndInternet, AdintScanner.MAX_RISKY_APP_POINTS)
                details.add("• ${result.appsWithLocationAndInternet} app(s) localisation+internet : +$points")
            }
            if (result.appsWithManyPermissions > 0) {
                val points = minOf(result.appsWithManyPermissions, AdintScanner.MAX_PERMISSION_HEAVY_POINTS)
                details.add("• ${result.appsWithManyPermissions} app(s) nombreuses permissions : +$points")
            }

            if (details.isEmpty()) {
                details.add("• Aucun facteur de risque détecté")
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

            // Fond de la barre
            val barBackground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xFFE0E0E0.toInt())
                cornerRadius = 12f
            }

            val barContainer = LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(600, 24)
                background = barBackground
            }

            // Barre de remplissage
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
     * Crée la carte des problèmes détectés
     */
    fun createProblemsCard(context: Context, problems: List<Problem>): LinearLayout {
        return createCard(context).apply {
            // Titre
            addView(TextView(context).apply {
                text = "PROBLÈMES DÉTECTÉS (${problems.size})"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF333333.toInt())
                setPadding(0, 0, 0, 16)
            })

            if (problems.isEmpty()) {
                addView(TextView(context).apply {
                    text = "Aucun problème détecté"
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

    /**
     * Crée un élément de problème
     */
    private fun createProblemItem(context: Context, problem: Problem): LinearLayout {
        val icon = when (problem.severity) {
            Severity.CRITICAL -> "●"  // Rouge
            Severity.WARNING -> "●"   // Orange
            Severity.INFO -> "●"      // Bleu
        }
        val iconColor = when (problem.severity) {
            Severity.CRITICAL -> 0xFFF44336.toInt()
            Severity.WARNING -> 0xFFFF9800.toInt()
            Severity.INFO -> 0xFF2196F3.toInt()
        }

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)

            // Icône
            addView(TextView(context).apply {
                text = icon
                textSize = 16f
                setTextColor(iconColor)
                setPadding(0, 0, 16, 0)
            })

            // Texte
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
            // Titre
            addView(TextView(context).apply {
                text = "ACTIONS RECOMMANDÉES"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF333333.toInt())
                setPadding(0, 0, 0, 16)
            })

            // Action 1: Réinitialiser l'ID pub
            if (result.gaid != null) {
                addView(createActionButton(
                    context,
                    "Réinitialiser l'ID publicitaire",
                    "Ouvre les paramètres de confidentialité",
                    Settings.ACTION_PRIVACY_SETTINGS
                ))
            }

            // Action 2: Désactiver pub perso
            if (!result.limitTracking) {
                addView(createActionButton(
                    context,
                    "Désactiver la personnalisation",
                    "Limite le suivi publicitaire",
                    Settings.ACTION_PRIVACY_SETTINGS
                ))
            }

            // Action 3: Vérifier les apps
            if (result.appsWithLocationAndInternet > 0) {
                addView(createActionButton(
                    context,
                    "Vérifier ${result.appsWithLocationAndInternet} app(s) à risque",
                    "Gérer les permissions des applications",
                    Settings.ACTION_APPLICATION_SETTINGS
                ))
            }

            // Action 4: Localisation
            addView(createActionButton(
                context,
                "Paramètres de localisation",
                "Contrôler l'accès à votre position",
                Settings.ACTION_LOCATION_SOURCE_SETTINGS
            ))

            // Action 5: DNS privé (Android 9+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                addView(createActionButton(
                    context,
                    "Configurer un DNS privé",
                    "Améliore la confidentialité réseau",
                    Settings.ACTION_WIRELESS_SETTINGS
                ))
            }
        }
    }

    /**
     * Crée un bouton d'action vers les paramètres système
     */
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

            // Conteneur texte
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )

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

            // Bouton flèche
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
                        // Fallback vers les paramètres généraux
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            })
        }
    }

    /**
     * Crée une carte d'identifiant (réutilisation du style existant)
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
            // Label
            addView(TextView(context).apply {
                text = label
                textSize = 14f
                setTextColor(0xFF777777.toInt())
            })

            // Valeur
            addView(TextView(context).apply {
                text = value ?: "Non disponible"
                textSize = 16f
                setTypeface(Typeface.MONOSPACE)
                setTextColor(0xFF000000.toInt())
                setPadding(0, 8, 0, 16)
            })

            // Description
            addView(TextView(context).apply {
                text = description
                textSize = 14f
                setPadding(4, 4, 4, 12)
                setTextColor(0xFF777777.toInt())
            })

            // Bouton de réinitialisation (optionnel)
            if (showResetButton && onResetClick != null) {
                addView(Button(context).apply {
                    text = "Supprimer l'Advertising ID"
                    setBackgroundColor(Color.RED)
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
}
