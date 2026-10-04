# Blockov – mode extraction shooter pour Minecraft 1.21.11 (Fabric)

Mod inspiré d'Arena Breakout, construit autour des classes client d'origine
(`dev.blockov.client.CData`, `CData$Extract`, `CData$Notice`). Ces classes ont été
reconstruites à l'identique depuis le bytecode ; tout le reste (serveur, réseau, HUD)
a été écrit pour leur fournir des données.

## Gameplay

- **Raid chronométré** : `/raid start [minutes]` (20 min par défaut). Apparition sur un point aléatoire.
- **Extractions** : se tenir à moins de 4 blocs d'un point d'extraction pendant 8 s. Certaines coûtent des *koens*.
- **Santé par partie du corps** : tête, thorax, ventre, bras, jambes. Tête ou thorax à 0 = mort.
  Les dégâts sur un membre détruit se répartissent sur le reste du corps. Jambe détruite = lenteur,
  bras détruit = faiblesse. Les projectiles touchent selon la hauteur d'impact, les chutes touchent les jambes.
  On récupère lentement quand on est bien nourri.
- **Poids** : au-delà de 30 kg, lenteur I ; au-delà de 45 kg, lenteur II.
- **Fin de raid** :
  - `EXTRAIT` : les objets de valeur (diamants, émeraudes, lingots…) sont revendus en koens, le reste est conservé, et on gagne de l'XP.
  - `TUE` : le butin tombe au sol.
  - `PORTE DISPARU` : le temps est écoulé, on a fait `/raid leave` ou on s'est déconnecté. Le butin est perdu.
- **Profil persistant** : niveau (1000 XP/niveau), koens, raids, extractions, morts, kills (`/raid profile`).
- **HUD** : chrono, état du corps, poids, extractions (distance/coût), barre d'extraction,
  notifications, indicateur de direction des dégâts, hitmarker (rouge = kill), écran de fin de raid.

## Commandes admin (op niveau 2)

```
/raid extract add <nom> <cout>   # extraction à votre position
/raid extract remove <nom>
/raid extract list
/raid spawn add                  # point d'apparition à votre position
/raid spawn clear
```

Les données sont enregistrées dans `<monde>/blockov.json`.

## Compiler

```
./gradlew build          # jar dans build/libs/
./gradlew runClient      # lancer un client de test
```

Il faut Java 21. Versions testées : Fabric Loader 0.19.5, Fabric API 0.141.6+1.21.11, Loom 1.14.10.

## Structure

```
src/main/java/dev/blockov/
  Blockov.java              point d'entrée serveur/commun
  inv/Profile.java, It.java progression et objets
  combat/Health.java        santé localisée
  net/                      paquet JSON unique serveur -> client
  raid/                     RaidManager (logique), Raid, Extract, Loot, Store (sauvegarde)
  command/RaidCommand.java
src/client/java/dev/blockov/client/
  CData.java                état client (reconstruit depuis les .class fournis)
  BlockovClient.java        réception réseau
  Hud.java                  affichage
```
