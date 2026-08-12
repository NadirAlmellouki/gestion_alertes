Tu dois maintenant reprendre le projet AlertOps pour une phase de VALIDATION, CORRECTION et ALIGNEMENT avant de continuer le développement des prochaines phases.

IMPORTANT :
Cette fois, tu dois réellement analyser le projet et ensuite modifier le code lorsque cela est nécessaire. Ne te limite pas à produire un rapport théorique.

============================================================
1. SOURCES À LIRE OBLIGATOIREMENT
============================================================

Commence par lire attentivement toutes les ressources pertinentes du dossier :

/info

En particulier, tu DOIS lire :

1. La Master Prompt du projet
   → elle se trouve dans /info
   → c'est la référence principale pour comprendre le besoin métier, les règles fonctionnelles et les concepts du système.

2. MLD_Global.txt
   → modèle logique de données de référence.

3. schema.sql
   → schéma PostgreSQL de référence.
   → IMPORTANT : ce fichier a déjà été exécuté avec succès dans le conteneur PostgreSQL actuel.
   → Considère donc ce schéma comme le schéma réellement choisi pour la base actuelle.
   → Ne pars PAS du principe que PostgreSQL contient uniquement les tables créées par Flyway V1–V4.

4. Le fichier Excel du référentiel métier.
   → analyse réellement toutes les feuilles et leurs colonnes.
   → ne te contente pas de regarder quelques lignes.

5. JSON_Alert_Dynatrace.json
   → analyse la structure réelle du payload Dynatrace utilisé par le projet.

6. Les autres fichiers de /info pertinents :
   → README
   → roadmap
   → documents d'architecture
   → documents de progression
   → exemples JSON
   → tout document qui permet de comprendre une décision déjà prise.

Tu peux et DOIS également inspecter l'ensemble du repository :
- src/
- pom.xml
- docker-compose.yml
- Dockerfile
- migrations Flyway
- application.yml
- application-*.yml
- .env.example / variables d'environnement disponibles
- configuration Keycloak
- configuration Kafka
- configuration PostgreSQL
- scripts
- tests
- configuration Docker
- etc.

Tu as accès aux ressources nécessaires du projet. Utilise-les réellement au lieu de supposer leur contenu.

============================================================
2. RÈGLE PRINCIPALE : COHÉRENCE > ARCHITECTURE THÉORIQUE
============================================================

Ne suis PAS aveuglément l'architecture décrite dans les documents.

La Master Prompt définit principalement le BESOIN MÉTIER.

Le MLD/schema.sql définissent principalement le MODÈLE DE DONNÉES.

Le code actuel définit les choix techniques déjà implémentés.

Ton objectif est de produire un système COHÉRENT entre :

Master Prompt
    ↓
besoin métier
    ↓
MLD
    ↓
schema.sql
    ↓
PostgreSQL réel
    ↓
JPA / repositories
    ↓
services / use cases
    ↓
API
    ↓
LLM / RAG
    ↓
tests

Si tu trouves une contradiction entre ces éléments :

1. identifie-la ;
2. comprends la raison de la contradiction ;
3. choisis la solution la plus cohérente et raisonnable ;
4. privilégie la simplicité et la maintenabilité ;
5. ne crée pas de complexité uniquement pour respecter une architecture théorique.

Ne transforme PAS le projet en architecture académique inutilement complexe.

Le but est d'avoir une application réellement fonctionnelle, cohérente et maintenable.

============================================================
3. MASTER PROMPT : RÉFÉRENCE MÉTIER
============================================================

La Master Prompt située dans /info est obligatoire à lire avant toute modification.

Elle doit être utilisée pour vérifier :

- les concepts métier ;
- les relations entre les concepts ;
- les responsabilités du LLM ;
- les responsabilités de PostgreSQL ;
- le rôle du Rules Engine ;
- la classification ;
- le routage ;
- les notifications ;
- les données fixes du référentiel ;
- les données dynamiques provenant de Dynatrace ;
- les règles de sécurité ;
- les rôles utilisateurs ;
- etc.

Mais attention :

Ne considère pas chaque détail d'implémentation de la Master Prompt comme une obligation technique absolue.

Si une décision technique actuelle est meilleure ou plus simple, conserve-la à condition qu'elle reste cohérente avec le besoin métier.

============================================================
4. RÔLES : EXACTEMENT 2 RÔLES
============================================================

C'est une correction importante.

Le système possède actuellement EXACTEMENT DEUX rôles métier :

- OPS
- SUPERVISOR

Il n'existe PAS de troisième rôle.

Toutes les anciennes références à :

- OPERATEUR
- SUPERVISEUR
- ADMINISTRATEUR
- ADMIN
- ou toute combinaison représentant trois rôles

doivent être considérées comme obsolètes si elles ne correspondent pas au système actuel.

Tu dois rechercher ces anciennes références dans :

- /info
- documentation
- README
- roadmap
- Java
- tests
- Keycloak realm
- configuration
- frontend
- exemples
- scripts

et les corriger lorsqu'elles sont encore présentes.

Le système final doit être cohérent partout :

OPS
SUPERVISOR

Ne crée PAS un troisième rôle uniquement parce qu'un ancien document en mentionne trois.

============================================================
5. BASE DE DONNÉES : schema.sql EST LA RÉFÉRENCE ACTUELLE
============================================================

Le fichier :

/info/schema.sql

a déjà été exécuté avec succès dans PostgreSQL.

Il ne faut donc PAS considérer Flyway V1–V4 comme l'unique représentation de la base.

Tu dois comparer :

- MLD_Global.txt
- /info/schema.sql
- les tables réellement utilisées par le code JPA
- les migrations Flyway
- les repositories
- les queries SQL
- PostgreSQL actuel si nécessaire.

Mais ne détruis PAS le schéma existant simplement pour faire correspondre Flyway.

Si Flyway historique et schema.sql divergent :

- ne modifie pas arbitrairement les anciennes migrations déjà exécutées ;
- ne supprime pas des tables simplement parce qu'elles ne correspondent pas aux anciennes migrations ;
- détermine quelle structure est réellement utilisée et nécessaire ;
- crée de nouvelles migrations si nécessaire ;
- garde une stratégie cohérente pour les environnements futurs.

Le point important est que le projet doit finalement avoir UNE représentation cohérente du modèle de données.

============================================================
6. MAINTENANCE_WINDOW_ENTITY
============================================================

Tu dois corriger le problème identifié précédemment concernant :

MaintenanceWindowEntity

Hibernate cherche actuellement une table du type :

maintenance_window_entity

alors que cette table n'existe pas dans le schéma SQL de référence.

IMPORTANT :

NE CRÉE PAS une table artificielle appelée maintenance_window_entity uniquement pour satisfaire Hibernate.

Si cette fonctionnalité n'est pas encore implémentée :

- retire le mapping JPA prématuré ;
- ou rends le stub non-JPA ;
- ou utilise une autre solution propre permettant de conserver ddl-auto=validate.

Applique le même raisonnement aux autres stubs JPA qui pourraient créer des tables inexistantes.

Ne désactive PAS simplement :

ddl-auto=validate

pour cacher le problème.

Le système doit démarrer parce que ses mappings correspondent réellement au schéma disponible.

============================================================
7. EXCEL → POSTGRESQL : À IMPLÉMENTER RÉELLEMENT
============================================================

C'est une priorité de cette tâche.

Le fichier Excel contient le référentiel métier réel.

Tu dois l'analyser complètement.

Identifie :

- les feuilles ;
- les colonnes ;
- les relations implicites ;
- les doublons ;
- les valeurs nulles ;
- les données actives/inactives ;
- les personnes ;
- les entités ;
- les pôles ;
- les domaines ;
- les solutions ;
- les responsables ;
- les rôles administratifs ;
- le PSI ;
- les autres attributs métier.

Ensuite, construis le mapping réel :

Excel
  ↓
PostgreSQL

en utilisant les tables définies dans schema.sql.

NE crée PAS une deuxième base de données parallèle au schéma existant.

NE crée PAS des tables inutiles juste pour importer l'Excel.

Utilise les tables existantes du modèle.

============================================================
8. IMPORT EXCEL : EXIGENCE D'IDEMPOTENCE
============================================================

L'import doit être réexécutable.

Exemple :

Premier lancement :

Excel
→ INSERT

Deuxième lancement :

Excel
→ UPDATE / aucune duplication

Troisième lancement :

Excel
→ même résultat cohérent

Il ne doit pas créer :

- plusieurs personnes identiques ;
- plusieurs entités identiques ;
- plusieurs solutions identiques ;
- plusieurs relations identiques.

Utilise les contraintes uniques et les clés appropriées déjà présentes dans schema.sql.

Si une donnée Excel est invalide :

- ne plante pas silencieusement ;
- journalise l'erreur ;
- continue lorsque c'est possible ;
- produit un résumé de l'import.

Exemple de résultat attendu :

X personnes importées
Y entités importées
Z pôles importés
...
N lignes rejetées

============================================================
9. HIÉRARCHIE MÉTIER
============================================================

L'analyse du fichier Excel doit être utilisée pour confirmer la hiérarchie réelle.

La structure métier observée est :

Entité
  ↓
Pôle
  ↓
Domaine
  ↓
Solution

Une solution appartient à un domaine.
Un domaine appartient à un pôle.
Un pôle appartient à une entité.

Le modèle PostgreSQL doit permettre cette hiérarchie sans coder en dur exactement trois niveaux.

Si schema.sql utilise une relation parent/enfant, exploite-la correctement.

Ne transforme pas cette hiérarchie en enums ou logique rigide inutilement.

============================================================
10. SOURCE DE VÉRITÉ DU RÉFÉRENTIEL
============================================================

Après l'import :

PostgreSQL devient la source de vérité du référentiel métier.

Le fichier JSON temporaire :

business-context/solutions-catalog.json

ne doit plus être considéré comme la source de vérité en production.

Il peut rester utile pour :

- tests ;
- développement isolé ;
- fallback temporaire si réellement nécessaire.

Mais la classification réelle doit pouvoir récupérer les données métier depuis PostgreSQL.

============================================================
11. RAG / RETRIEVAL : À IMPLÉMENTER
============================================================

La classification Gemini doit être enrichie par les données PostgreSQL.

Ne fais PAS :

Dynatrace JSON
→ Gemini directement

La cible doit être :

Dynatrace
    ↓
AlertContextExtractor
    ↓
contexte technique pertinent
    ↓
retrieval PostgreSQL
    ↓
Top-K solutions candidates
    ↓
prompt enrichi
    ↓
Gemini
    ↓
classification
    ↓
validation
    ↓
lookup PostgreSQL

Le retrieval doit utiliser les informations pertinentes provenant du payload Dynatrace.

Exemples :

- title
- severity
- impact
- affected entities
- impacted entities
- root cause
- entity tags
- management zones
- Kubernetes
- evidence
- autres champs réellement pertinents après analyse du JSON.

Ne mets PAS tout le JSON Dynatrace dans le prompt.

Construis un contexte compact et pertinent.

============================================================
12. RETRIEVAL : COMMENCER SIMPLE
============================================================

Pour la première version, ne mets PAS Elasticsearch, vector database ou une infrastructure RAG inutilement complexe.

Une recherche PostgreSQL raisonnable suffit.

Par exemple :

- recherche textuelle ;
- ILIKE ;
- correspondance sur nom de solution ;
- domaine ;
- pôle ;
- entité ;
- tags ;
- mots-clés issus de Dynatrace ;
- score de pertinence.

Retourne un nombre limité de candidats, par exemple :

Top-K = 5 à 8

La valeur exacte doit être choisie selon les données réelles et la taille du prompt.

Si une amélioration fuzzy avec pg_trgm est réellement utile, tu peux l'utiliser.

Mais ne rajoute pas de technologie externe simplement parce que le terme "RAG" est utilisé.

Un RAG peut parfaitement commencer par un retrieval PostgreSQL.

============================================================
13. PROMPT GEMINI : À REFAIRE SÉRIEUSEMENT
============================================================

La prompt actuelle est jugée trop pauvre.

Tu dois analyser :

- AlertContextExtractor
- BusinessContextPort
- FileBusinessContextAdapter
- ClassificationPromptBuilder
- ClassificationResponseValidator
- ClassificationResult
- GeminiClassifierAdapter
- AlertClassifiedEvent

puis reconstruire la chaîne si nécessaire.

Le prompt doit clairement séparer :

SYSTEM INSTRUCTIONS
ALERT CONTEXT
RETRIEVED BUSINESS CONTEXT
TASK
OUTPUT CONTRACT
RESTRICTIONS

Le LLM doit comprendre :

1. ce qu'est une alerte Dynatrace ;
2. les informations techniques disponibles ;
3. les candidats métier récupérés depuis PostgreSQL ;
4. qu'il doit choisir parmi les candidats fournis ;
5. qu'il ne doit PAS inventer une solution ;
6. qu'il doit pouvoir retourner "aucun candidat fiable" ;
7. qu'il doit fournir une confiance ;
8. qu'il doit produire un résumé exploitable ;
9. qu'il ne doit PAS calculer le PSI ;
10. qu'il ne doit PAS décider du routage ;
11. qu'il ne doit PAS choisir un administrateur ;
12. qu'il ne doit PAS décider quel membre doit être contacté.

Le LLM doit faire uniquement la classification prévue par le métier.

============================================================
14. PSI : RESPONSABILITÉ DE POSTGRESQL
============================================================

Point extrêmement important.

Le LLM ne doit jamais produire :

proposedPriority
priority
PSI calculé
PSI proposé

Le PSI est une donnée métier fixe du référentiel.

Le processus doit être :

Gemini
→ sélectionne une solution candidate

Application
→ valide que cette solution appartient aux candidats récupérés

PostgreSQL
→ récupère solution_attribute.psi

Donc :

LLM = classification

PostgreSQL = données métier de référence

Rules Engine = logique métier

Routing Engine = routage

Ne mélange pas ces responsabilités.

============================================================
15. VALIDATION DU RESULTAT GEMINI
============================================================

Le validator doit vérifier au minimum :

- JSON valide ;
- structure correcte ;
- champs obligatoires ;
- confidence valide ;
- matchedSolution éventuellement null ;
- matchedSolution appartient réellement aux candidats envoyés au LLM ;
- aucune solution inventée ;
- aucune priorité inventée ;
- aucune information de routage ;
- cohérence minimale entre les informations retournées et le contexte fourni.

Si le LLM retourne une solution qui n'était pas dans les candidats :

→ résultat invalide / fallback

Ne jamais accepter automatiquement une solution inconnue.

============================================================
16. DONNÉES ADMINISTRATIVES ET LLM
============================================================

Ne mets pas dans le prompt de classification des données qui ne sont pas nécessaires.

En particulier :

- emails des administrateurs ;
- personnes responsables ;
- informations de routage ;
- canaux de notification ;
- disponibilités.

Ces données appartiennent au routage et à la notification.

Le LLM de classification n'a pas besoin de savoir qui contacter.

============================================================
17. DYNATRACE : ANALYSER CE QUI DOIT ÊTRE UTILISÉ
============================================================

Consulte réellement :

/info/JSON_Alert_Dynatrace.json

et les fixtures Dynatrace disponibles.

Détermine quels champs sont réellement utiles pour :

1. ingestion ;
2. normalisation ;
3. retrieval ;
4. classification ;
5. timeline ;
6. corrélation future.

Ne suppose pas que tous les champs doivent être envoyés à Gemini.

Le contexte envoyé au LLM doit être le résultat d'une sélection raisonnée.

============================================================
18. NE PAS COMMENCER LE RULES ENGINE
============================================================

Tant que les corrections suivantes ne sont pas terminées :

- BDD cohérente ;
- JPA cohérent ;
- schema.sql cohérent avec le code ;
- import Excel fonctionnel ;
- référentiel PostgreSQL disponible ;
- retrieval fonctionnel ;
- prompt Gemini correctement enrichi ;
- validator corrigé ;
- tests de classification fonctionnels ;

NE COMMENCE PAS la Phase 4 Rules Engine.

La priorité actuelle est de stabiliser les Phases 0–3.

============================================================
19. DOCUMENTATION : CORRIGER LES ANCIENNES INFORMATIONS
============================================================

Après les corrections techniques, recherche dans tout le projet les informations obsolètes.

En particulier :

OPERATEUR
SUPERVISEUR
ADMINISTRATEUR
ADMIN
/dev/login
JWT local
user_role_mappings
ProposedPriority
LiteLlmHttpClient
LiteLlmClassifierAdapter
etc.

Corrige les documents qui ne correspondent plus au système actuel.

Attention :
ne supprime pas une information simplement parce qu'elle est ancienne si elle décrit volontairement un historique ou une migration.

Dans ce cas, indique clairement qu'il s'agit d'une ancienne architecture.

Les documents opérationnels doivent cependant décrire l'état actuel.

============================================================
20. FLYWAY
============================================================

Ne modifie PAS aveuglément V1, V2, V3 ou V4 si elles ont déjà été exécutées.

Les migrations déjà appliquées doivent être considérées comme historiques.

Si une modification de schéma est nécessaire :

→ crée une nouvelle migration.

Mais avant de créer une migration, vérifie le schéma réellement choisi dans :

/info/schema.sql

et assure-toi qu'il n'existe pas déjà une table ou colonne correspondante.

Ne crée pas de doublons.

============================================================
21. TESTS
============================================================

Après chaque correction importante, exécute les tests appropriés.

Au minimum :

- tests unitaires ;
- tests de persistence si disponibles ;
- tests de classification ;
- tests de validation Gemini ;
- tests d'import Excel ;
- tests du retrieval ;
- tests de démarrage Spring Boot ;
- Actuator health.

Ajoute des tests là où il manque une couverture importante.

En particulier :

1. import Excel idempotent ;
2. solution retrouvée parmi les candidats ;
3. solution inexistante ;
4. faible confiance ;
5. JSON Gemini invalide ;
6. Gemini retourne une solution hors candidats ;
7. PSI récupéré depuis PostgreSQL après classification ;
8. aucun PSI généré par Gemini.

============================================================
22. ATTENTION AUX DONNÉES RÉELLES
============================================================

Ne fabrique pas des données métier lorsqu'elles existent déjà dans :

/info

Utilise les données réelles de l'Excel.

Ne remplace pas les noms de solutions par des exemples inventés.

Ne crée pas arbitrairement :

Solution A
Solution B
Domaine X
etc.

sauf dans les tests unitaires où des fixtures synthétiques sont explicitement nécessaires.

============================================================
23. ORDRE D'EXÉCUTION
============================================================

Travaille dans cet ordre :

ÉTAPE 1
Lire Master Prompt + MLD + schema.sql + Excel + Dynatrace JSON + code.

ÉTAPE 2
Faire un audit réel du schéma et du code.

ÉTAPE 3
Corriger les stubs JPA qui empêchent le démarrage, notamment MaintenanceWindowEntity.

ÉTAPE 4
Aligner AlertEntity / repositories / persistence avec schema.sql.

ÉTAPE 5
Vérifier les migrations Flyway sans casser l'historique.

ÉTAPE 6
Implémenter l'import Excel → PostgreSQL.

ÉTAPE 7
Vérifier les données réellement importées et l'idempotence.

ÉTAPE 8
Implémenter PostgresBusinessContextAdapter.

ÉTAPE 9
Implémenter le retrieval Top-K.

ÉTAPE 10
Refaire AlertContextExtractor si nécessaire à partir du vrai JSON Dynatrace.

ÉTAPE 11
Refaire ClassificationPromptBuilder avec le contexte Dynatrace + candidats PostgreSQL.

ÉTAPE 12
Corriger ClassificationResponseValidator.

ÉTAPE 13
Supprimer toute notion de PSI calculé/proposé par Gemini.

ÉTAPE 14
Récupérer le PSI depuis PostgreSQL après classification.

ÉTAPE 15
Mettre à jour les tests.

ÉTAPE 16
Corriger la documentation obsolète, notamment les rôles.

ÉTAPE 17
Lancer les tests complets et vérifier que Spring Boot démarre.

============================================================
24. IMPORTANT : NE TE CONTENTE PAS D'UN RAPPORT
============================================================

Cette demande n'est PAS uniquement une demande d'audit.

Tu dois :

1. analyser ;
2. identifier les problèmes ;
3. corriger les problèmes ;
4. écrire le code nécessaire ;
5. créer les migrations nécessaires si elles sont réellement nécessaires ;
6. implémenter l'import Excel ;
7. implémenter le retrieval PostgreSQL ;
8. enrichir le prompt Gemini ;
9. corriger le validator ;
10. ajouter les tests ;
11. corriger les documents obsolètes.

Lorsque tu rencontres une décision ambiguë, choisis la solution la plus raisonnable et cohérente avec :

Master Prompt
+ MLD
+ schema.sql
+ données Excel
+ JSON Dynatrace
+ code actuel.

Ne bloque pas toute l'implémentation pour des détails secondaires.

============================================================
25. RÈGLE FINALE
============================================================

Le résultat recherché n'est PAS :

"une architecture parfaite sur papier".

Le résultat recherché est :

UN SYSTÈME COHÉRENT ET FONCTIONNEL.

Les responsabilités doivent être clairement séparées :

Dynatrace
→ fournit les données techniques

PostgreSQL
→ fournit le référentiel métier

RAG / Retrieval
→ sélectionne les données métier pertinentes

Gemini
→ classe l'alerte à partir du contexte fourni

Validator
→ vérifie que Gemini respecte le contrat

Application
→ récupère les données fixes comme PSI

Rules Engine
→ applique les règles métier

Routing Engine
→ décide du routage

Notification
→ exécute la communication

Et actuellement, nous nous concentrons uniquement sur la stabilisation des Phases 0–3.

Commence maintenant par lire la Master Prompt dans /info et toutes les ressources mentionnées ci-dessus, puis inspecte le code actuel avant de modifier quoi que ce soit.