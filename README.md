# LISA

*Lecture Intelligente & Synthèse Adaptée*

Application Android qui affiche une bulle flottante (façon "chat head") par-dessus les autres
apps. Quand tu tapes dessus, elle lit ce qui est affiché à l'écran (page web, email, document),
te propose de le résumer, et t'explique en quoi ça touche à tes objectifs et centres d'intérêt
déclarés dans ton profil.

## Fonctionnement

1. Une bulle flottante reste visible par-dessus n'importe quelle app (`BubbleOverlayService`).
2. Un service d'accessibilité (`ScreenContentAccessibilityService`) lit en continu le texte
   affiché à l'écran de l'app au premier plan et devine le type de contenu (page web / email /
   document) à partir du package de l'app.
3. Au tap sur la bulle, la dernière capture connue est proposée : "Tu veux que je résume ça ?".
4. Sur "Oui", le contenu est passé à un moteur de résumé (`SummarizationEngine`) avec le profil
   utilisateur, et le résultat (résumé court + pertinence personnelle) s'affiche dans une carte.

## Architecture

```
app/src/main/java/com/lisa/app/
├── MainActivity.kt              # Onboarding permissions, navigation Accueil/Profil
├── data/                        # UserProfile + persistance locale (DataStore)
├── domain/                      # CapturedContent, SummaryResult, SummarizationEngine
├── service/                     # BubbleOverlayService, ScreenContentAccessibilityService
├── overlay/                     # UI Compose de la bulle (état replié / prompt / résultat)
├── ui/                          # Écrans Compose (Accueil, Profil, Onboarding) + thème
└── util/                        # Vérification des permissions
```

Le résumé passe par l'interface `SummarizationEngine`. L'implémentation actuelle
(`MockSummarizationEngine`) est un simple bouchon local (raccourcissement de texte +
correspondance de mots-clés), pensé pour être remplacé par un vrai LLM sans changer le reste
de l'app.

## Permissions requises

- **Affichage par-dessus les autres apps** (`SYSTEM_ALERT_WINDOW`) — pour la bulle flottante.
- **Service d'accessibilité** — pour lire le texte affiché à l'écran (aucune capture d'écran,
  aucun accès réseau depuis ce service : uniquement l'arbre d'accessibilité, comme un lecteur
  d'écran).

Les deux se configurent depuis l'écran d'accueil de l'app au premier lancement.

## Build

```
./gradlew assembleDebug
```

> Ce dépôt a été écrit dans un environnement sans SDK Android ni accès réseau à Maven ; le
> build n'y a donc pas pu être validé. À builder/tester sur une machine avec Android Studio
> ou le SDK Android installé.

## Limitations actuelles

- Le résumé est mocké (pas d'appel LLM) : c'est une implémentation "architecture d'abord".
- Le statut de la bulle (activée/désactivée) n'est pas persisté au-delà du processus.
- Pas de tests automatisés.

## Extensions possibles

- **Vrai moteur de résumé** : brancher un LLM (ex. API Claude) derrière `SummarizationEngine`,
  avec le profil utilisateur injecté dans le prompt pour personnaliser la pertinence.
- **Historique des résumés** : garder une liste consultable des contenus résumés (avec lien vers
  la source), pour retrouver un résumé plus tard.
- **Filtrage par app / par plage horaire** : ne proposer un résumé que sur certaines apps
  (navigateur, email) ou à certains moments (pas pendant le travail, par exemple).
- **Actions rapides depuis le résultat** : "Enregistrer", "Partager", "Ajouter à mes tâches" en
  plus de "Fermer".
- **Résumé vocal** : lecture à voix haute du résumé (utile en mobilité), via TTS.
- **Apprentissage des préférences** : ajuster automatiquement le profil (objectifs/intérêts)
  à partir des contenus que l'utilisateur choisit de résumer ou d'ignorer.
- **Multi-langue** : détection de la langue du contenu et résumé dans la langue préférée de
  l'utilisateur.
- **Mode "digest"** : au lieu d'un résumé immédiat, regrouper plusieurs contenus lus dans la
  journée en un digest unique envoyé le soir.
- **Widget / notification enrichie** : afficher le dernier résumé directement dans une
  notification, sans avoir à rouvrir la carte.
- **Synchronisation multi-appareils** : profil et historique partagés entre téléphone et
  tablette/web, si l'usage dépasse un seul appareil.
