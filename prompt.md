# 13. Gestion Git et commits intermédiaires

Pendant toute l'implémentation, utilise Git de manière propre et structurée.

### Commits obligatoires

Ne fais pas un seul énorme commit à la fin. Crée un commit après chaque **partie fonctionnelle importante et stable**.

Exemples de commits attendus :

1. `feat(rules): complete business rules evaluation`
2. `feat(routing): complete routing and escalation engine`
3. `feat(notification): implement email notification workflow`
4. `feat(sms): implement Kafka SMS producer`
5. `feat(voip): implement local VoIP simulation`
6. `feat(audit): implement audit logging`
7. `feat(alert): complete alert history and lifecycle`
8. `feat(dashboard): implement supervisor and ops dashboards`
9. `feat(frontend): integrate React frontend with backend`
10. `test: complete backend integration tests`

Les noms exacts peuvent être adaptés à ce qui est réellement implémenté.

### Règles importantes pour les commits

- Ne committe **que du code qui compile et fonctionne** autant que possible.
- Avant chaque commit important :
  - vérifier que le projet compile ;
  - exécuter les tests pertinents ;
  - vérifier les migrations Flyway ;
  - vérifier que Docker Compose démarre correctement si la modification concerne l'infrastructure ;
  - vérifier qu'aucune fonctionnalité précédente n'a été cassée.
- Ne jamais faire de commit contenant volontairement du code cassé simplement pour sauvegarder l'état.
- Ne pas utiliser `git reset --hard`, `git clean -fd` ou toute autre commande destructive sans mon autorisation explicite.
- Ne pas supprimer ou écraser une fonctionnalité existante simplement parce qu'elle est plus facile à remplacer.
- Avant une modification architecturale importante, examiner d'abord le code existant et expliquer brièvement pourquoi cette modification est nécessaire.
- Après chaque commit important, indique clairement :
  - le commit créé ;
  - les fichiers/modules principaux modifiés ;
  - ce qui fonctionne maintenant ;
  - les tests exécutés ;
  - les éventuels problèmes restant à traiter.MISSION — FINALISATION COMPLÈTE DU PROJET ALERTOPS

Tu es maintenant chargé de FINALISER le projet AlertOps de bout en bout.

L'objectif est d'obtenir aujourd'hui une version fonctionnelle, cohérente, testable et démontrable de l'application, avec un backend terminé et le frontend suffisamment complet pour exploiter réellement les fonctionnalités disponibles.

Tu as accès au projet complet, au code Java/Spring Boot, au frontend React existant, au dossier `info/`, aux fichiers de configuration, aux migrations PostgreSQL, aux tests et aux conteneurs Docker déjà utilisés par le projet.

Tu dois travailler comme un développeur senior chargé de terminer une application existante, et non comme quelqu'un qui doit simplement ajouter quelques classes.

============================================================
0. RÈGLE FONDAMENTALE : COMPRENDRE AVANT DE MODIFIER
============================================================

Avant toute modification importante :

1. Analyse l'architecture actuelle.
2. Analyse les modules/packages existants.
3. Analyse les ports/adapters existants.
4. Analyse les entités et les migrations PostgreSQL.
5. Analyse les services et use cases existants.
6. Analyse les tests existants.
7. Analyse le fichier `info/prototype.jsx`.
8. Analyse les autres fichiers du dossier `info/`.
9. Analyse les configurations Docker existantes.
10. Analyse les configurations `application.yml`, `application-*.yml`, etc.
11. Identifie ce qui est réellement déjà fonctionnel.
12. Identifie ce qui est seulement un squelette.
13. Identifie les incohérences entre le code, la BDD, le prototype et les documents.

IMPORTANT :

Le dossier `info/` est principalement une SOURCE DE CONTEXTE.

Il sert à comprendre :

- le fonctionnement attendu ;
- les formats JSON ;
- les règles métier ;
- les règles de routage ;
- les données Dynatrace ;
- les formats SMS ;
- ElevenLabs ;
- le prototype ;
- les décisions fonctionnelles ;
- l'état d'avancement.

NE COPIE PAS aveuglément tout ce qui est dans `info/`.

Si une information de `info/` est ancienne, incomplète ou contradictoire avec l'architecture actuelle, analyse le problème et choisis la solution techniquement cohérente.

Tu es AUTORISÉ, lorsque cela est réellement nécessaire, à modifier :

- Java ;
- Spring Boot ;
- packages ;
- classes ;
- interfaces ;
- ports ;
- adapters ;
- repositories ;
- entités ;
- DTO ;
- services ;
- controllers ;
- événements ;
- migrations Flyway ;
- tables PostgreSQL ;
- configurations YAML ;
- Docker Compose ;
- frontend React ;
- prototype ;
- tests ;
- conception interne.

Mais :

NE MODIFIE PAS UNE PARTIE QUI FONCTIONNE SANS RAISON.

Toute modification d'architecture doit avoir une justification technique.

Le but n'est pas de respecter artificiellement l'ancienne conception.

Le but est d'obtenir une application réellement fonctionnelle.

============================================================

1. OBJECTIF GLOBAL

============================================================

À la fin de ton travail, le flux principal doit être capable de fonctionner de manière cohérente :

Dynatrace
   ↓
Réception de l'alerte
   ↓
Validation / normalisation
   ↓
Persistance PostgreSQL
   ↓
Classification
   ↓
LLM si nécessaire
   ↓
Business Rules Engine
   ↓
Business Result
   ↓
Routing Engine
   ↓
Routing Decision
   ↓
Notification
   ├── EMAIL
   ├── SMS / Kafka
   └── VOIP
          ↓
       Escalade
          ↓
       Suivi / résolution
          ↓
       Audit / journalisation

Chaque étape doit être traçable.

============================================================
2. CLASSIFICATION ET DONNÉES DYNATRACE
============================================================

Analyse le module Classification existant.

Ne suppose PAS que toutes les informations métier sont produites par le LLM.

Le JSON Dynatrace contient déjà des informations importantes, par exemple :

- problemId ;
- displayId ;
- title ;
- status ;
- severityLevel ;
- impactLevel ;
- affectedEntities ;
- impactedEntities ;
- rootCauseEntity ;
- entityTags ;
- managementZones ;
- etc.

Tu dois distinguer clairement :

A. Informations directement disponibles dans Dynatrace.

B. Informations calculées par notre application.

C. Informations réellement produites par le LLM.

D. Informations produites par le moteur de règles.

Le LLM ne doit pas être utilisé pour quelque chose qui est déjà disponible de manière fiable dans Dynatrace.

Si une donnée peut être extraite directement du payload Dynatrace, privilégie cette approche.

Le LLM doit être utilisé seulement lorsqu'une interprétation/classification est réellement nécessaire.

============================================================
3. BUSINESS RULE ENGINE
============================================================

Le moteur de règles métier est une partie CENTRALE du projet.

Analyse complètement :

- BusinessRuleEngine ;
- ConditionEvaluator ;
- BusinessRule ;
- BusinessRuleContext ;
- RuleCondition ;
- RuleConditionGroup ;
- RuleAction ;
- BusinessDecision ;
- RuleOrigin ;
- les repositories ;
- les entités de persistence ;
- les migrations ;
- les tests existants.

Il existe deux types de règles :

1. DEFAULT
2. CONFIGURED

Les règles CONFIGURED doivent avoir priorité sur les règles DEFAULT lorsque la conception actuelle le prévoit.

Les règles DEFAULT constituent le comportement de base de l'application.

Le moteur doit pouvoir :

- charger les règles actives ;
- respecter leur priorité/ordre ;
- évaluer les conditions ;
- gérer les groupes AND/OR ;
- produire les actions ;
- produire un BusinessDecision ;
- conserver la traçabilité de la règle déclenchée ;
- journaliser l'exécution ;
- gérer l'absence de règle correspondante ;
- gérer les cas d'erreur.

Vérifie également que les règles par défaut correspondent réellement au comportement métier attendu.

NE TE LIMITE PAS aux tests existants.

Les tests existants montrent une partie du comportement attendu, mais ils ne constituent pas nécessairement toute la spécification.

============================================================
4. CONFIGURATION DES RÈGLES MÉTIER
============================================================

La configuration des règles doit être réellement persistante.

Vérifie :

BDD
 ↓
BusinessRule
 ↓
Conditions
 ↓
ConditionGroups
 ↓
Actions
 ↓
BusinessRuleEngine

Une règle créée/modifiée depuis la configuration doit réellement être utilisée par le moteur.

Il ne faut PAS avoir :

Frontend
 ↓
fausse configuration
 ↓
BDD

alors que le moteur Java continue d'utiliser des règles codées en dur.

La configuration doit réellement influencer l'exécution.

Si la conception actuelle ne permet pas cela, corrige-la.

============================================================
5. ROUTING ENGINE
============================================================

Analyse complètement le moteur de routage.

Il doit être réellement connecté au résultat du Business Rule Engine.

Flux attendu :

BusinessDecision
        ↓
Routing Engine
        ↓
Routing Policy
        ↓
Organizational Unit
        ↓
Members
        ↓
Channel
        ↓
Escalation Policy
        ↓
Routing Decision

Vérifie :

- priorité des politiques ;
- politiques DEFAULT ;
- politiques CONFIGURED si présentes ;
- unité organisationnelle ;
- membres ;
- disponibilité ;
- ordre des administrateurs ;
- canaux ;
- niveaux d'escalade ;
- timeout ;
- nombre maximal de tentatives ;
- action terminale.

Le routage ne doit pas être simplement simulé dans une méthode.

Il doit utiliser réellement les données configurées.

============================================================
6. EMAIL
============================================================

Implémente/finalise l'envoi d'e-mail.

Le service Mail est déjà disponible dans Docker.

Analyse sa configuration existante.

Le backend doit :

- sélectionner le destinataire selon le Routing Decision ;
- construire le message ;
- envoyer l'e-mail ;
- enregistrer la tentative ;
- enregistrer le résultat technique ;
- gérer les erreurs ;
- journaliser l'opération.

IMPORTANT :

Un e-mail envoyé ne signifie PAS que l'alerte est "prise en charge".

Il faut distinguer :

EMAIL_SENT
EMAIL_DELIVERY_FAILED
BUSINESS_ACKNOWLEDGED
INCIDENT_RESOLVED

Ne mélange pas ces états.

============================================================
7. SMS + KAFKA
============================================================

IMPORTANT :

NE CRÉE PAS un service SMS.

Le service SMS appartient à une autre infrastructure/service de l'entreprise.

Notre application est seulement PRODUCTEUR Kafka.

Kafka est déjà disponible dans Docker.

Il existe déjà :

- le cluster Kafka ;
- Kafka UI.

Notre responsabilité :

AlertOps
   ↓
Kafka Producer
   ↓
topic SMS
   ↓
JSON SMS attendu par l'entreprise

Le format JSON SMS est déjà disponible dans le dossier `info/`.

Utilise ce format comme référence.

Le backend doit être capable de produire exactement le payload attendu.

Il doit :

- construire le JSON ;
- récupérer les informations nécessaires ;
- envoyer le message au topic ;
- gérer les erreurs Kafka ;
- journaliser l'envoi ;
- enregistrer le message/correlationId si nécessaire.

NE CRÉE PAS un fake SMS provider.

Kafka est notre intégration réelle.

============================================================
8. PROBLÈME IMPORTANT : SUIVI APRÈS SMS / EMAIL
============================================================

C'est une partie importante du projet.

Contrairement au VoIP :

VoIP :
    appel
      ↓
    admin répond
      ↓
    confirmation possible

SMS / EMAIL :

```
envoi
  ↓
pas nécessairement d'accusé métier
  ↓
impossible de savoir directement si l'admin a pris en charge
  ↓
il faut donc suivre l'état de l'incident dans Dynatrace
```

NE DÉDUIS PAS :

SMS_SENT = ALERT_HANDLED

C'est faux.

NE DÉDUIS PAS :

EMAIL_SENT = ALERT_HANDLED

C'est également faux.

Le système doit distinguer :

1. notification technique envoyée ;
2. notification technique échouée ;
3. incident toujours actif ;
4. incident résolu ;
5. timeout de suivi ;
6. éventuellement escalade.

============================================================
9. VÉRIFICATION DU STATUS DYNATRACE
============================================================

Très important :

Le webhook Dynatrace est utilisé pour recevoir l'alerte initiale.

Mais pour vérifier ultérieurement si le problème est toujours actif/résolu, ne dépends pas uniquement du webhook initial.

Il faut utiliser l'API Dynatrace de consultation du problème lorsque c'est nécessaire.

Le principe attendu est :

T0 :
Dynatrace → Webhook → AlertOps

Puis :

T0 + délai configurable
        ↓
AlertOps → Dynatrace API
        ↓
récupération du statut actuel
        ↓
status RESOLVED ?
        ├── YES → incident résolu
        └── NO  → toujours actif → continuer surveillance / escalade

IMPORTANT :

NE CHOISIS PAS arbitrairement un délai fixe sans analyser le projet.

Recherche dans :

- code ;
- configuration ;
- dossier info ;
- règles ;
- prototype ;
- documentation existante ;

s'il existe déjà une notion de :

- timeout ;
- escalation delay ;
- retry interval ;
- polling interval ;
- notification timeout ;
- max attempts.

Si elle existe, réutilise-la.

Si elle n'existe pas, introduis une configuration explicite.

Par exemple :

alertops:
  monitoring:
    resolution-check:
      initial-delay: ...
      polling-interval: ...
      max-duration: ...
      max-attempts: ...

Les valeurs doivent être CONFIGURABLES.

Ne hardcode pas les délais métier.

============================================================
10. MÉCANISME DE SUIVI
============================================================

Choisis la meilleure approche technique selon l'architecture actuelle.

Possibilités :

- Scheduled task ;
- Spring Scheduler ;
- job périodique ;
- événement + scheduled retry ;
- autre mécanisme cohérent.

Le mécanisme doit être robuste.

Exemple :

Notification envoyée
        ↓
NotificationState = SENT
        ↓
création d'un suivi
        ↓
attente du délai
        ↓
Dynatrace API
        ↓
status ?
        ├── RESOLVED
        │      ↓
        │  clôturer le suivi
        │
        └── ACTIVE
               ↓
        appliquer la politique d'escalade
               ↓
        nouvelle notification
               ↓
        nouveau contrôle

Évite les boucles bloquantes dans les requêtes HTTP.

Ne fais pas attendre un thread HTTP pendant plusieurs minutes.

Le suivi doit être asynchrone.

============================================================
11. VOIP
============================================================

Le projet doit utiliser une abstraction.

Le domaine/application ne doit PAS dépendre directement d'un fournisseur.

Créer/utiliser un port du type :

VoiceCallPort
ou
VoipNotificationPort

Le moteur de routage doit seulement demander :

call(phoneNumber, message)

Il ne doit pas connaître :

- Asterisk ;
- SIP ;
- WebRTC ;
- Twilio ;
- etc.

============================================================
12. VOIP LOCAL POUR LA DÉMONSTRATION
============================================================

Nous avons choisi une simulation VoIP locale avec Docker.

Objectif :

AlertOps
 ↓
VoipPort
 ↓
Asterisk / PBX local
 ↓
SIP/WebRTC
 ↓
Softphone navigateur

Nous voulons pouvoir démontrer :

- appel entrant ;
- sonnerie ;
- réponse ;
- refus ;
- raccrochage ;
- état de l'appel ;
- acquittement.

Utilise des extensions SIP fictives.

La table Person contient déjà un champ `phone`.

Exemple :

Person:
1001
1002
1003

Ces valeurs représentent les extensions internes de test.

NE DUPLIQUE PAS les personnes métier dans Asterisk.

Asterisk connaît seulement ses extensions SIP.

AlertOps connaît les personnes.

La correspondance se fait via `Person.phone`.

============================================================
13. DOCKER VOIP
============================================================

Ajoute les conteneurs nécessaires dans :

docker-compose.override.yaml

pour permettre la démonstration locale du VoIP.

Tu peux modifier le Docker Compose existant si nécessaire.

Le système doit être reproductible.

Documente clairement :

- ports ;
- volumes ;
- configuration SIP ;
- WebRTC ;
- extensions ;
- credentials de test ;
- réseau Docker ;
- interaction avec Spring Boot.

Ne crée pas une architecture inutilement complexe.

Si une solution plus simple et fiable permet d'obtenir le même résultat, utilise-la.

============================================================
14. ELEVENLABS / TTS
============================================================

Le projet utilise ElevenLabs pour le TTS.

Les informations nécessaires existent déjà dans le dossier `info/`.

Analyse-les.

NE RECOPIE PAS les credentials dans le code.

Utilise :

- variables d'environnement ;
- configuration externe ;
- secrets ;
- application.yml avec placeholders.

Flux :

Routing Decision
      ↓
Voice notification
      ↓
Generate voice message
      ↓
ElevenLabs TTS
      ↓
Audio
      ↓
VoIP
      ↓
Admin

Le texte envoyé à ElevenLabs doit être généré à partir du contexte réel de l'alerte et de la décision métier.

============================================================
15. ESCALADE
============================================================

Finalise l'escalade.

Cas général :

Notification
 ↓
attente
 ↓
incident résolu ?
 ├── oui → FIN
 └── non
      ↓
niveau suivant
      ↓
nouvelle notification

Pour VoIP, il existe une possibilité supplémentaire :

CALLING
 ↓
ANSWERED ?
 ├── oui → confirmation / prise en charge
 └── non → tentative suivante

Pour SMS/EMAIL :

SENT
 ↓
pas d'ACK métier direct
 ↓
vérification Dynatrace
 ↓
RESOLVED ?
 ├── oui → terminé
 └── non → escalade

Cette différence doit être représentée dans le code.

============================================================
16. JOURNALISATION / AUDIT
============================================================

Finalise la partie audit/logging.

Il faut pouvoir répondre à :

- quelle alerte ?
- quand ?
- quelle classification ?
- quelle règle ?
- quelle décision métier ?
- quelle politique de routage ?
- quel destinataire ?
- quel canal ?
- quelle tentative ?
- quel résultat ?
- quelle escalade ?
- quelle résolution ?
- quelle erreur ?

Évite de créer des dépendances absurdes entre Audit et tous les modules.

Privilégie une journalisation centralisée avec des événements/identifiants permettant de retracer le workflow.

Utilise des correlation IDs / alert IDs / notification IDs lorsque pertinent.

L'audit doit être consultable depuis le frontend.

============================================================
17. POSTGRESQL / FLYWAY
============================================================

Analyse les tables existantes.

NE CRÉE PAS de nouvelles tables si une table existante peut être correctement utilisée.

Mais si le modèle actuel ne permet pas de réaliser le suivi, l'escalade ou l'audit correctement, modifie le schéma.

Utilise Flyway pour les migrations.

Chaque changement BDD doit avoir sa migration.

Vérifie :

- FK ;
- indexes ;
- contraintes ;
- enums/statuts ;
- timestamps ;
- UUID ;
- relations ;
- cohérence avec les entités JPA.

La BDD doit refléter le modèle réel du workflow.

============================================================
18. TESTS BACKEND
============================================================

Finalise les tests.

Il faut au minimum couvrir :

CLASSIFICATION

- payload normal ;
- confiance faible ;
- fallback ;
- erreur LLM.

BUSINESS RULES

- règle configurée prioritaire ;
- règle default ;
- conditions ;
- AND ;
- OR ;
- absence de règle ;
- action métier.

ROUTING

- politique trouvée ;
- politique absente ;
- membre disponible ;
- membre indisponible ;
- escalade ;
- niveau parent ;
- terminal action.

EMAIL

- envoi réussi ;
- erreur.

KAFKA

- payload SMS ;
- publication ;
- erreur.

VOIP

- appel ;
- ANSWERED ;
- REJECTED ;
- NO_ANSWER ;
- HANGUP ;
- escalade.

RESOLUTION MONITORING

- incident RESOLVED ;
- incident ACTIVE ;
- timeout ;
- nouvelle vérification ;
- arrêt du polling.

AUDIT

- événement journalisé ;
- corrélation correcte.

Utilise Mockito/AssertJ comme déjà utilisé dans le projet lorsque cela est pertinent.

============================================================
19. FRONTEND
============================================================

Après avoir stabilisé le backend, complète le frontend existant.

Le frontend principal est ici :

C:\Users\DELL\Desktop\PFA\frontEnd

Analyse d'abord ce qui existe.

NE SUPPRIME PAS inutilement la base React existante.

Tu dois connecter réellement le frontend aux APIs backend.

============================================================
20. PROTOTYPE
============================================================

Le fichier :

info/prototype.jsx

sert de référence visuelle et fonctionnelle.

Il contient certaines pages déjà conçues.

Mais il est INCOMPLET.

Il manque notamment des éléments/pages autour de :

- Alertes ;
- Historique ;
- Dashboard ;
- Statistiques ;
- fonctionnalités superviseur ;
- journalisation/audit ;
- consultation des traitements.

Tu dois compléter ce qui manque.

IMPORTANT :

NE MODIFIE PAS arbitrairement les pages concernant la configuration des moteurs de règles/routage si elles correspondent déjà au comportement attendu.

Concentre les améliorations frontend principalement sur :

- affichage des alertes ;
- détails d'une alerte ;
- historique ;
- audit ;
- dashboard ;
- statistiques ;
- suivi des notifications ;
- suivi des escalades ;
- fonctionnalités superviseur ;
- consultation des résultats.

============================================================
21. DASHBOARD
============================================================

Le dashboard actuel est trop pauvre.

Améliore-le significativement.

Il doit donner une vision opérationnelle.

Exemples de statistiques utiles :

- nombre total d'alertes ;
- alertes critiques ;
- alertes ouvertes ;
- alertes résolues ;
- alertes en attente de validation ;
- taux de résolution ;
- alertes nécessitant une intervention humaine ;
- notifications envoyées ;
- notifications échouées ;
- SMS ;
- emails ;
- appels VoIP ;
- escalades ;
- temps moyen de traitement ;
- répartition par catégorie ;
- répartition par sévérité ;
- répartition par application ;
- évolution temporelle.

N'ajoute pas toutes ces statistiques si les données backend ne permettent pas de les calculer correctement.

Le frontend ne doit PAS inventer des données.

S'il manque une API backend pour une statistique réellement utile, crée l'API correspondante.

============================================================
22. PAGE ALERTES
============================================================

Créer/compléter une vraie page de supervision.

Elle doit permettre au minimum :

- liste des alertes ;
- statut ;
- sévérité ;
- application ;
- catégorie ;
- date ;
- confiance IA ;
- statut de résolution ;
- notification ;
- escalade ;
- détail.

Ajouter filtrage/recherche/pagination si cohérent avec le projet.

============================================================
23. HISTORIQUE
============================================================

Créer une vue permettant de comprendre le cycle de vie :

ALERTE
 ↓
CLASSIFICATION
 ↓
RULE
 ↓
BUSINESS DECISION
 ↓
ROUTING
 ↓
NOTIFICATION
 ↓
ESCALATION
 ↓
RESOLUTION

Afficher les événements de manière claire.

============================================================
24. SUPERVISEUR
============================================================

Compléter les fonctions utiles du superviseur.

Le superviseur doit pouvoir consulter notamment :

- alertes ;
- alertes nécessitant validation ;
- notifications ;
- historique ;
- audit ;
- escalades ;
- décisions ;
- éventuellement réaffectation si le backend la supporte.

Ne crée pas des fonctions fictives qui ne sont pas supportées par le backend.

============================================================
25. AUTHENTIFICATION
============================================================

Respecte le mécanisme d'authentification déjà présent dans le projet.

Ne contourne pas la sécurité uniquement pour simplifier le frontend.

Si certains endpoints doivent être protégés, vérifie :

- rôles ;
- superviseur ;
- opérateur ;
- permissions.

============================================================
26. INTÉGRATION FRONTEND/BACKEND
============================================================

Le frontend doit utiliser réellement les APIs backend.

Pas de données hardcodées pour simuler les statistiques.

Pas de fausses alertes permanentes.

Pas de fake routing decision.

Pas de faux historique.

Les données doivent venir du backend.

Si une API manque :

1. identifier le besoin ;
2. créer le endpoint ;
3. créer DTO ;
4. service ;
5. repository si nécessaire ;
6. test ;
7. connecter React.

============================================================
27. CONFIGURATION
============================================================

Toutes les informations sensibles doivent être externalisées.

NE COMMITTE PAS :

- API keys ;
- tokens ;
- passwords ;
- secrets.

Utilise les variables d'environnement.

Exemples :

ELEVENLABS_API_KEY
DYNATRACE_API_TOKEN
DYNATRACE_URL
KAFKA_BOOTSTRAP_SERVERS
MAIL_HOST
MAIL_PORT
VOIP_SIP_USER
VOIP_SIP_PASSWORD

Adapte les noms à la convention existante.

============================================================
28. DOCKER
============================================================

Les conteneurs existants doivent être réutilisés.

Tu as déjà notamment :

- PostgreSQL ;
- Kafka ;
- Kafka UI ;
- service Mail ;
- autres services existants.

NE DUPLIQUE PAS ces services.

Ajoute seulement ce qui manque réellement.

Pour VoIP, ajoute les conteneurs nécessaires.

Vérifie que tous les services communiquent correctement sur le réseau Docker.

============================================================
29. API DYNATRACE
============================================================

Analyse les fichiers `info/` pour trouver :

- URL ;
- endpoints ;
- authentification ;
- exemples ;
- payload ;
- IDs ;
- status.

Ne suppose pas l'API si les informations du projet la documentent déjà.

Si l'API n'est pas suffisamment documentée dans le projet, identifie clairement ce qui manque et construis une abstraction permettant de compléter facilement la configuration.

Créer si nécessaire :

DynatraceProblemPort

puis :

DynatraceApiAdapter

Le métier ne doit pas dépendre directement du client HTTP Dynatrace.

============================================================
30. ARCHITECTURE À RESPECTER
============================================================

Privilégie :

Domain
Application
Infrastructure
Adapters
Ports

Exemple :

Business logic
      ↓
Port
      ↓
Adapter
      ↓
Infrastructure externe

Même principe pour :

- Kafka ;
- Email ;
- Dynatrace ;
- VoIP ;
- ElevenLabs.

Ne mélange pas la logique métier avec les appels HTTP externes.

============================================================
31. OBSERVABILITÉ
============================================================

Ajoute des logs utiles.

Exemples :

alertId
problemId
notificationId
ruleId
routingDecisionId
correlationId

Les logs doivent permettre de comprendre :

"Pourquoi cette alerte a-t-elle été envoyée à cette personne avec ce canal ?"

Évite les logs inutiles et les logs contenant des secrets.

============================================================
32. GESTION DES ERREURS
============================================================

Chaque intégration externe peut échouer.

Prévoir :

Dynatrace indisponible
LLM indisponible
ElevenLabs indisponible
Kafka indisponible
Mail indisponible
VoIP indisponible

Le système ne doit pas planter globalement parce qu'un service externe est temporairement indisponible.

Utilise selon le besoin :

- retry ;
- timeout ;
- fallback ;
- état FAILED ;
- escalade ;
- logs ;
- audit.

Ne mets pas de retry infini.

============================================================
33. MIGRATIONS
============================================================

Après modification du modèle :

- créer migration Flyway ;
- démarrer PostgreSQL ;
- exécuter migrations ;
- vérifier les tables ;
- vérifier les FK ;
- vérifier les données par défaut ;
- vérifier les règles DEFAULT ;
- vérifier les politiques DEFAULT.

============================================================
34. DONNÉES DEFAULT
============================================================

Le système doit démarrer avec un comportement minimal fonctionnel.

Vérifie les données par défaut :

- règles métier DEFAULT ;
- politiques de routage DEFAULT ;
- unités organisationnelles de test ;
- membres de test ;
- canaux ;
- paramètres d'escalade.

Ces données doivent être cohérentes avec le comportement du moteur.

============================================================
35. MODE D'EXÉCUTION
============================================================

Ne te contente PAS de compiler.

Tu dois réellement :

1. compiler ;
2. lancer les tests ;
3. lancer Docker Compose ;
4. vérifier PostgreSQL ;
5. vérifier Kafka ;
6. vérifier Kafka UI ;
7. vérifier Mail ;
8. vérifier VoIP ;
9. lancer Spring Boot ;
10. vérifier les endpoints ;
11. vérifier le frontend ;
12. effectuer un scénario complet.

============================================================
36. SCÉNARIO DE TEST FINAL OBLIGATOIRE
============================================================

Construis et exécute un scénario réel :

1. Envoyer une alerte Dynatrace de test.
2. Vérifier sa réception.
3. Vérifier sa persistance.
4. Vérifier sa classification.
5. Vérifier la décision métier.
6. Vérifier la règle déclenchée.
7. Vérifier la décision de routage.
8. Vérifier le destinataire.
9. Vérifier le canal.
10. Si EMAIL :
  vérifier l'envoi vers le service Mail.
11. Si SMS :
  vérifier le JSON envoyé dans Kafka.
12. Si VOIP :
  vérifier l'appel via l'infrastructure VoIP locale.
13. Vérifier l'escalade si nécessaire.
14. Vérifier la consultation du statut Dynatrace.
15. Simuler/observer la résolution.
16. Vérifier l'arrêt de l'escalade.
17. Vérifier l'audit.
18. Vérifier l'affichage frontend.

============================================================
37. SI QUELQUE CHOSE EST MAL CONÇU
============================================================

Tu as explicitement le droit de modifier la conception.

Exemple :

Si tu découvres que :

RoutingEngine
    ↓
NotificationService
    ↓
DB

ne permet pas de gérer correctement l'escalade,

tu peux modifier cette conception.

Si une table est insuffisante, modifie-la.

Si une classe mélange trop de responsabilités, refactorise-la.

Si un port est mal placé, déplace-le.

Si un événement est inutile, supprime-le.

Si un événement est nécessaire, ajoute-le.

Si un endpoint manque, ajoute-le.

Mais évite les refactorings cosmétiques.

Chaque changement doit servir à rendre le système fonctionnel, maintenable et cohérent.

============================================================
38. PRIORITÉS
============================================================

Travaille dans cet ordre :

## PRIORITÉ 1

Business Rules Engine
Routing Engine
Configuration réelle BDD → moteur

## PRIORITÉ 2

Notification workflow
Email
Kafka SMS

## PRIORITÉ 3

Resolution Monitoring Dynatrace
Escalade
Suivi asynchrone

## PRIORITÉ 4

VoIP Docker + SIP/WebRTC
ElevenLabs TTS

## PRIORITÉ 5

Audit / historique / journalisation

## PRIORITÉ 6

APIs nécessaires au frontend

## PRIORITÉ 7

Frontend React
Dashboard
Alertes
Historique
Superviseur
Statistiques

## PRIORITÉ 8

Tests d'intégration
Docker
Scénario complet

============================================================
39. IMPORTANT : NE PAS S'ARRÊTER À LA PREMIÈRE ERREUR
============================================================

Si tu rencontres une erreur :

1. analyse la cause ;
2. corrige ;
3. relance ;
4. continue.

Ne me demande pas de résoudre manuellement une erreur qui peut être résolue raisonnablement en inspectant le projet.

Tu peux prendre des décisions techniques.

Si une information manque réellement et qu'elle est impossible à déduire du projet, indique précisément :

- ce qui manque ;
- où cela manque ;
- pourquoi c'est nécessaire ;
- quelle valeur/configuration est attendue.

============================================================
40. CRITÈRE FINAL DE RÉUSSITE
============================================================

Je ne veux pas seulement :

"le projet compile".

Je veux :

- backend compilable ;
- tests OK ;
- BDD cohérente ;
- migrations OK ;
- règles métier réellement exécutables ;
- règles configurées réellement utilisées ;
- routage réellement exécuté ;
- email fonctionnel ;
- Kafka SMS fonctionnel ;
- JSON SMS correct ;
- vérification Dynatrace fonctionnelle ;
- escalade fonctionnelle ;
- VoIP locale fonctionnelle ;
- TTS ElevenLabs intégré ;
- audit fonctionnel ;
- APIs disponibles ;
- frontend connecté ;
- dashboard utile ;
- alertes consultables ;
- historique consultable ;
- fonctionnalités superviseur cohérentes ;
- Docker fonctionnel ;
- scénario complet démontrable.

============================================================
41. RAPPORT FINAL DE TON TRAVAIL
============================================================

À la fin, donne-moi un résumé structuré :

A. Ce qui existait déjà
B. Ce que tu as ajouté
C. Ce que tu as modifié
D. Ce que tu as supprimé
E. Les changements BDD
F. Les nouveaux endpoints
G. Les nouveaux services
H. Les nouveaux ports/adapters
I. Les nouveaux conteneurs Docker
J. Les configurations nécessaires
K. Les tests exécutés
L. Résultat des tests
M. Scénario de démonstration
N. Ce qui reste éventuellement impossible à tester localement
O. Commandes exactes pour démarrer tout le projet

Pour chaque problème restant, ne dis pas simplement "à faire".

Explique précisément pourquoi il reste et quelle est la prochaine action.

============================================================
42. Gestion Git et commits intermédiaires
============================================================

Pendant toute l'implémentation, utilise Git de manière propre et structurée.

Commits obligatoires

Ne fais pas un seul énorme commit à la fin. Crée un commit après chaque partie fonctionnelle importante et stable.

Exemples de commits attendus :

feat(rules): complete business rules evaluation
feat(routing): complete routing and escalation engine
feat(notification): implement email notification workflow
feat(sms): implement Kafka SMS producer
feat(voip): implement local VoIP simulation
feat(audit): implement audit logging
feat(alert): complete alert history and lifecycle
feat(dashboard): implement supervisor and ops dashboards
feat(frontend): integrate React frontend with backend
test: complete backend integration tests

Les noms exacts peuvent être adaptés à ce qui est réellement implémenté.

Règles importantes pour les commits
Ne committe que du code qui compile et fonctionne autant que possible.
Avant chaque commit important :
vérifier que le projet compile ;
exécuter les tests pertinents ;
vérifier les migrations Flyway ;
vérifier que Docker Compose démarre correctement si la modification concerne l'infrastructure ;
vérifier qu'aucune fonctionnalité précédente n'a été cassée.
Ne jamais faire de commit contenant volontairement du code cassé simplement pour sauvegarder l'état.
Ne pas utiliser git reset --hard, git clean -fd ou toute autre commande destructive sans mon autorisation explicite.
Ne pas supprimer ou écraser une fonctionnalité existante simplement parce qu'elle est plus facile à remplacer.
Avant une modification architecturale importante, examiner d'abord le code existant et expliquer brièvement pourquoi cette modification est nécessaire.
Après chaque commit important, indique clairement :
le commit créé ;
les fichiers/modules principaux modifiés ;
ce qui fonctionne maintenant ;
les tests exécutés ;
les éventuels problèmes restant à traiter.

Stratégie recommandée

Travaille par blocs fonctionnels cohérents, par exemple :

Analyse → implémentation → tests → validation → commit

puis passer au bloc suivant.

À la fin, fournir un résumé Git indiquant les commits créés dans l'ordre et ce que chacun représente.

# ============================================================

RÈGLE FINALE

Tu dois agir comme un ingénieur responsable de livrer une version fonctionnelle d'AlertOps.

Tu n'es PAS obligé de suivre aveuglément l'ancienne architecture.

Tu dois respecter :

- le domaine métier ;
- les responsabilités des modules ;
- les contraintes de l'entreprise ;
- les données réelles ;
- les interfaces externes ;
- la sécurité ;
- la cohérence BDD ;
- la maintenabilité.

Si une meilleure conception est nécessaire, adopte-la.

Mais ne transforme pas le projet en une architecture inutilement complexe.

PRIVILÉGIE :
Simplicité + cohérence + testabilité + fonctionnement réel.

COMMENCE MAINTENANT PAR L'ANALYSE COMPLÈTE DU PROJET AVANT DE MODIFIER LE CODE.