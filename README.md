# Ma Pilule — Android natif

Application locale sans permission Internet : confirmation quotidienne, historique de 30 jours, NIP de 4 à 6 chiffres et rappel configurable (10 h par défaut).

## Compiler sans Android Studio

1. Décompresser le ZIP et placer **son contenu**, et non le ZIP, à la racine du dépôt GitHub.
2. Ouvrir **Actions > Build Android APK > Run workflow** (ou pousser un commit sur `main`).
3. Une fois le workflow terminé, ouvrir le run et télécharger **MaPilule-APK** dans Artifacts.
4. Décompresser l'artifact pour obtenir `app-debug.apk`, puis l'installer sur Android.

Le workflow utilise Gradle 8.10.2 installé par GitHub Actions : aucun wrapper requis.

## Limites importantes

- Le rappel est demandé à 10 h par défaut mais peut être retardé par Android/OxygenOS (Doze, batterie, permission d'alarmes exactes).
- Sur Android 13+, autoriser les notifications dans Réglages de l'application.
- Les données restent sur le téléphone. Désinstaller l'application efface l'historique et le NIP.
- Le NIP protège l'interface ; ce n'est pas un chiffrement de la base de données.
- APK de débogage : destiné à l'installation personnelle, pas à une publication Play Store.
- Ce projet a été reconstruit depuis un export incomplet. Une compilation GitHub Actions est nécessaire pour valider l'APK.
