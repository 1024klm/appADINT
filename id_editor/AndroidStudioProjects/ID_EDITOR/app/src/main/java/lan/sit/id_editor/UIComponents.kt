package lan.sit.id_editor

import android.content.ClipData
import android.content.ClipboardManager
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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

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
     * Crée la carte du score de durcissement ADINT
     */
    fun createScoreCard(context: Context, result: AdintResult, scanner: AdintScanner): LinearLayout {
        val protectionLevel = scanner.getProtectionLevel(result.score)

        return createCard(context).apply {
            // Titre avec mention "heuristique"
            addView(TextView(context).apply {
                text = "SCORE DE DURCISSEMENT (heuristique)"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF333333.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 8)
            })

            // Sous-titre explicatif
            addView(TextView(context).apply {
                text = "10 = bien protégé, 0 = non protégé"
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
                setTextColor(protectionLevel.color)
                gravity = Gravity.CENTER
            })

            // Niveau de protection
            addView(TextView(context).apply {
                text = "PROTECTION ${protectionLevel.label.uppercase()}"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(protectionLevel.color)
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 24)
            })

            // Barre de progression
            addView(createScoreBar(context, result.score, protectionLevel.color))

            // Détails du calcul
            addView(TextView(context).apply {
                text = "Pénalités détectées :"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF555555.toInt())
                setPadding(0, 24, 0, 8)
            })

            // Liste des pénalités
            val details = mutableListOf<String>()
            if (!result.limitAdTrackingEnabled) {
                details.add("• LAT non activé : -${AdintScanner.POINTS_LAT_DISABLED}")
            }
            val gaidValid = result.gaid != null && result.gaid != "00000000-0000-0000-0000-000000000000"
            if (gaidValid) {
                details.add("• GAID accessible : -${AdintScanner.POINTS_GAID_ACCESSIBLE}")
            }
            if (result.appsWithLocationAndInternet > 0) {
                val points = minOf(result.appsWithLocationAndInternet, AdintScanner.MAX_RISKY_APP_POINTS)
                details.add("• ${result.appsWithLocationAndInternet} app(s) loc+net : -$points")
            }
            if (result.appsWithManyPermissions > 0) {
                val points = minOf(result.appsWithManyPermissions, AdintScanner.MAX_PERMISSION_HEAVY_POINTS)
                details.add("• ${result.appsWithManyPermissions} app(s) >10 perms : -$points")
            }

            if (details.isEmpty()) {
                details.add("• Aucune pénalité - configuration optimale !")
            }

            addView(TextView(context).apply {
                text = details.joinToString("\n")
                textSize = 13f
                setTextColor(0xFF666666.toInt())
            })
        }
    }

    /**
     * Crée une barre de progression pour le score (largeur dynamique)
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

            // Utiliser weight pour largeur dynamique au lieu de 600px hardcodé
            val barContainer = LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(context, 20)
                ).apply {
                    marginStart = dpToPx(context, 16)
                    marginEnd = dpToPx(context, 16)
                }
                background = barBackground
            }

            // La barre de remplissage (seulement si score > 0)
            if (score > 0) {
                val barFill = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        score.toFloat()
                    )
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(color)
                        cornerRadius = 12f
                    }
                }
                barContainer.addView(barFill)
            }

            // Espace vide pour compléter à 10 (seulement si score < 10)
            if (score < 10) {
                val barEmpty = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (10 - score).toFloat()
                    )
                }
                barContainer.addView(barEmpty)
            }
            addView(barContainer)
        }
    }

    /**
     * Convertit dp en pixels (avec arrondi correct)
     */
    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    /**
     * Crée une carte d'erreur
     */
    fun createErrorCard(context: Context, title: String, message: String): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xFFFFCDD2.toInt())
                setStroke(2, 0xFFE57373.toInt())
                cornerRadius = 24f
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }

            addView(TextView(context).apply {
                text = title
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFFC62828.toInt())
            })

            addView(TextView(context).apply {
                text = message
                textSize = 14f
                setTextColor(0xFFD32F2F.toInt())
                setPadding(0, 8, 0, 0)
            })
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
     * Crée un bouton pour accéder à la page des limitations
     */
    fun createLimitationsButton(context: Context): Button {
        return Button(context).apply {
            text = "Limites de ce diagnostic"
            textSize = 13f
            setTextColor(0xFF6D4C41.toInt())

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xFFFFF8E1.toInt())
                setStroke(2, 0xFFFFE082.toInt())
                cornerRadius = 16f
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
                bottomMargin = 16
            }

            setOnClickListener {
                showLimitationsDialog(context)
            }
        }
    }

    /**
     * Affiche le dialogue des limitations techniques
     */
    fun showLimitationsDialog(context: Context) {
        val limitations = listOf(
            "Ne détecte PAS les SDK publicitaires intégrés" to
                "Les bibliothèques de tracking comme Facebook SDK, Google Analytics, etc. ne sont pas détectables sans analyse statique du code.",

            "Ne surveille PAS le trafic réseau réel" to
                "Cette app ne capture pas les requêtes réseau. Elle ne peut pas voir les données réellement envoyées par les applications.",

            "Ne détecte PAS le fingerprinting" to
                "Les techniques de fingerprinting (canvas, audio, WebGL, fonts...) permettent de vous identifier même sans cookies ni ID publicitaire.",

            "Le flag LAT est déclaratif" to
                "Activer 'Limit Ad Tracking' envoie une demande aux apps, mais rien ne les oblige à la respecter techniquement.",

            "Permissions déclarées ≠ utilisées" to
                "Une app peut déclarer une permission sans jamais l'utiliser, ou l'utiliser de manière bénigne.",

            "Prototype de laboratoire" to
                "Cette application est un outil pédagogique et de recherche. Elle n'est pas destinée à être publiée sur le Play Store."
        )

        val scrollView = ScrollView(context).apply {
            setPadding(48, 32, 48, 32)
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Titre
        layout.addView(TextView(context).apply {
            text = "LIMITES TECHNIQUES"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF5D4037.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        })

        // Sous-titre
        layout.addView(TextView(context).apply {
            text = "Ce que ce diagnostic ne peut PAS faire"
            textSize = 14f
            setTextColor(0xFF8D6E63.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        })

        // Liste des limitations avec détails
        limitations.forEach { (title, description) ->
            layout.addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16, 0, 16)

                // Icône + Titre
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL

                    addView(TextView(context).apply {
                        text = "⚠"
                        textSize = 16f
                        setPadding(0, 0, 12, 0)
                    })

                    addView(TextView(context).apply {
                        text = title
                        textSize = 14f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(0xFF5D4037.toInt())
                    })
                })

                // Description
                addView(TextView(context).apply {
                    text = description
                    textSize = 12f
                    setTextColor(0xFF8D6E63.toInt())
                    setPadding(28, 8, 0, 0)
                })
            })

            // Séparateur
            layout.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2
                ).apply {
                    topMargin = 8
                }
                setBackgroundColor(0xFFFFE082.toInt())
            })
        }

        scrollView.addView(layout)

        android.app.AlertDialog.Builder(context)
            .setView(scrollView)
            .setPositiveButton("Compris") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /**
     * Crée la carte des RECOMMANDATIONS DE DURCISSEMENT
     */
    fun createRecommendationsCard(context: Context): LinearLayout {
        return createCard(context).apply {
            // Fond vert pâle pour distinguer
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xFFE8F5E9.toInt()) // Vert pâle
                setStroke(2, 0xFFA5D6A7.toInt())
                cornerRadius = 24f
            }

            addView(TextView(context).apply {
                text = "RECOMMANDATIONS DE DURCISSEMENT"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF2E7D32.toInt())
                setPadding(0, 0, 0, 16)
            })

            // Recommandation 1: Localisation
            addView(createRecommendationItem(
                context,
                title = "Désactiver la géolocalisation",
                steps = "Paramètres → Localisation → Désactiver",
                description = "Empêche toutes les apps d'accéder à votre position GPS.",
                settingsAction = Settings.ACTION_LOCATION_SOURCE_SETTINGS
            ))

            // Recommandation 2: Supprimer GAID
            addView(createRecommendationItem(
                context,
                title = "Supprimer l'ID publicitaire",
                steps = "Paramètres → Confidentialité → Annonces → Supprimer",
                description = "Supprime définitivement votre identifiant de ciblage publicitaire.",
                settingsAction = "com.google.android.gms.settings.ADS_PRIVACY",
                fallbackAction = Settings.ACTION_PRIVACY_SETTINGS
            ))

            // Recommandation 3: Permissions
            addView(createRecommendationItem(
                context,
                title = "Auditer les permissions",
                steps = "Paramètres → Applications → Permissions",
                description = "Révoquez les permissions inutiles (localisation, contacts, etc.).",
                settingsAction = Settings.ACTION_APPLICATION_SETTINGS
            ))

            // Recommandation 4: DNS privé
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                addView(createRecommendationItem(
                    context,
                    title = "Activer le DNS privé",
                    steps = "Paramètres → Réseau → DNS privé → Activer",
                    description = "Chiffre vos requêtes DNS pour plus de confidentialité.",
                    settingsAction = Settings.ACTION_WIRELESS_SETTINGS
                ))
            }

            // Recommandation 5: Désinstaller apps inutiles
            addView(TextView(context).apply {
                text = "\nAutres recommandations :"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF388E3C.toInt())
                setPadding(0, 8, 0, 8)
            })

            val otherTips = listOf(
                "• Désinstaller les apps inutilisées",
                "• Éviter les apps demandant trop de permissions",
                "• Utiliser un navigateur respectueux de la vie privée",
                "• Désactiver le Bluetooth et WiFi quand non utilisés",
                "• Vérifier régulièrement les apps en arrière-plan"
            )

            addView(TextView(context).apply {
                text = otherTips.joinToString("\n")
                textSize = 12f
                setTextColor(0xFF43A047.toInt())
                setLineSpacing(4f, 1f)
            })
        }
    }

    /**
     * Crée un élément de recommandation avec bouton d'action
     */
    private fun createRecommendationItem(
        context: Context,
        title: String,
        steps: String,
        description: String,
        settingsAction: String,
        fallbackAction: String? = null
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)

            // Titre
            addView(TextView(context).apply {
                text = title
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF1B5E20.toInt())
            })

            // Étapes
            addView(TextView(context).apply {
                text = steps
                textSize = 12f
                setTypeface(Typeface.MONOSPACE)
                setTextColor(0xFF2E7D32.toInt())
                setPadding(0, 4, 0, 4)
            })

            // Description
            addView(TextView(context).apply {
                text = description
                textSize = 11f
                setTextColor(0xFF558B2F.toInt())
                setPadding(0, 0, 0, 8)
            })

            // Bouton
            addView(Button(context).apply {
                text = "Ouvrir les paramètres"
                textSize = 11f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(0xFF43A047.toInt())
                    cornerRadius = 8f
                }

                setOnClickListener {
                    try {
                        context.startActivity(Intent(settingsAction))
                    } catch (e: Exception) {
                        // Fallback si l'intent principal échoue
                        try {
                            context.startActivity(Intent(fallbackAction ?: Settings.ACTION_SETTINGS))
                        } catch (e2: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                }
            })
        }
    }

    /**
     * Crée une carte d'identifiant
     * @param realValue Valeur réelle (non masquée) pour la copie - si null, pas de bouton copier
     * @param actionHint Texte d'aide affiché sous le bouton d'action
     */
    fun createIdentifierCard(
        context: Context,
        label: String,
        displayValue: String?,
        description: String,
        realValue: String? = null,
        showActionButton: Boolean = false,
        actionButtonText: String = "Gérer",
        actionHint: String? = null,
        onActionClick: (() -> Unit)? = null
    ): LinearLayout {
        return createCard(context).apply {
            addView(TextView(context).apply {
                text = label
                textSize = 14f
                setTextColor(0xFF777777.toInt())
            })

            addView(TextView(context).apply {
                text = displayValue ?: "Non disponible"
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

            // Ligne de boutons
            val buttonsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Bouton Copier (si realValue fournie)
            if (realValue != null) {
                buttonsRow.addView(Button(context).apply {
                    text = "Copier"
                    textSize = 12f
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginEnd = 16 }

                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(0xFF607D8B.toInt())
                        cornerRadius = 8f
                    }

                    setOnClickListener {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(label, realValue)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copié dans le presse-papiers", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            // Bouton action (gérer, etc.)
            if (showActionButton && onActionClick != null) {
                buttonsRow.addView(Button(context).apply {
                    text = actionButtonText
                    textSize = 12f
                    setTextColor(Color.WHITE)

                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(0xFF2196F3.toInt())
                        cornerRadius = 8f
                    }

                    setOnClickListener { onActionClick() }
                })
            }

            if (buttonsRow.childCount > 0) {
                addView(buttonsRow)
            }

            // Texte d'aide sous le bouton
            if (actionHint != null) {
                addView(TextView(context).apply {
                    text = actionHint
                    textSize = 11f
                    setTextColor(0xFF888888.toInt())
                    setPadding(0, 8, 0, 0)
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
