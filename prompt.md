# PROMPT CURSOR — IMPLÉMENTATION COMPLÈTE DES RÈGLES MÉTIER ET DU ROUTAGE

Tu dois maintenant implémenter la partie centrale de l'application : **le système de règles métier et de routage**, avec à la fois :

1. les **règles métier par défaut** ;
2. les **règles de routage par défaut** ;
3. les **règles métier configurables par les Ops** ;
4. les **règles de routage configurables par les Ops** ;
5. l'exécution réelle de ces règles côté Java/Spring Boot ;
6. la persistance de leur configuration dans PostgreSQL ;
7. l'intégration avec le résultat de classification du LLM ;
8. l'intégration avec la hiérarchie organisationnelle et les personnes ;
9. l'intégration avec la notification et les étapes de routage ;
10. la traçabilité des décisions et des exécutions.

## 1. CONTEXTE FONCTIONNEL IMPORTANT

Le système reçoit une alerte Dynatrace.

Le LLM intervient principalement pour **analyser/classifier l'alerte** et proposer, parmi les candidats récupérés depuis PostgreSQL :

* une catégorie ;
* un type de problème ;
* une confidence ;
* une Solution candidate ;
* éventuellement un Domaine ;
* éventuellement un Pôle ;
* éventuellement une Entité ;
* un résumé ;
* une cause probable ;
* une justification ;
* les champs incertains ;
* un indicateur de fallback ;
* un indicateur de validation humaine.

Le LLM **ne doit pas décider** :

* de la priorité métier officielle ;
* du PSI ;
* des personnes à contacter ;
* de l'ordre des personnes ;
* du canal de communication ;
* de l'escalade ;
* du nombre de relances ;
* de la durée des relances ;
* de l'action finale à exécuter.

Le LLM fournit donc **des informations d'entrée pour la logique déterministe de l'application**.

Le reste doit être décidé par le système de règles.

---

# 2. DISTINCTION FONDAMENTALE À RESPECTER

Il existe deux domaines différents :

## A. BUSINESS RULE ENGINE

Il répond à :

> **« Que devons-nous décider concernant cette alerte ? »**

Exemples :

* quelle Solution utiliser ;
* quel contexte métier est retenu ;
* quelle politique métier appliquer ;
* faut-il demander une validation humaine ;
* quelle décision métier prendre à partir du résultat LLM + BDD ;
* quelles conditions métier sont satisfaites ;
* quelle action métier doit être produite.

Il ne doit pas devenir un moteur de routage déguisé.

---

## B. ROUTING ENGINE

Il répond à :

> **« Maintenant que nous savons quoi faire, vers qui et comment devons-nous agir ? »**

Exemples :

* trouver les personnes correspondant à un rôle métier ;
* résoudre un rôle au niveau Solution/Domaine/Pôle/Entité ;
* déterminer l'ordre des personnes ;
* appeler la première personne ;
* attendre le délai configuré ;
* passer à la personne suivante ;
* notifier un superviseur ;
* changer de canal ;
* gérer les réponses ACCEPTED / REJECTED / TIMEOUT ;
* terminer ou poursuivre le routage.

**Ne mélange jamais ces deux responsabilités.**

---

# 3. RÈGLES PAR DÉFAUT VS RÈGLES CONFIGURABLES

C'est un point essentiel.

Les règles par défaut ne sont PAS une « ancienne version » du moteur.

Elles font partie du **flux normal V0**.

Le comportement normal de l'application doit être défini par ces règles par défaut.

Les règles configurées par les Ops constituent des **branches configurables pouvant modifier ou remplacer le comportement par défaut lorsqu'une condition est satisfaite**.

Conceptuellement :

```text
ALERTE
  |
  v
Classification LLM
  |
  v
Contexte métier
  |
  v
BUSINESS RULE ENGINE
  |
  +---- règle configurable correspondante
  |          |
  |          v
  |     décision configurée
  |
  +---- aucune règle configurable
             |
             v
       décision par défaut
  |
  v
ROUTING ENGINE
  |
  +---- politique configurable correspondante
  |          |
  |          v
  |     routage configuré
  |
  +---- aucune politique correspondante
             |
             v
       routage par défaut
  |
  v
NOTIFICATION / ACTION
```

Il ne faut donc **jamais coder les règles par défaut uniquement dans des `if/else` Java impossibles à modifier**.

Le système doit permettre de représenter leur configuration et leur comportement de manière suffisamment structurée.

---

# 4. OBJECTIF PRINCIPAL

Implémente un véritable système où :

```text
LLM result
     +
Alert data
     +
Organization data
     +
Solution data
     +
Person / role data
     +
Availability
     +
Default rules
     +
Configurable rules
     ↓
Deterministic decision
     ↓
Routing decision
     ↓
Action execution
```

Les données doivent réellement circuler entre les composants.

Ne réalise pas uniquement le CRUD du frontend.

Je veux que la configuration créée dans l'interface Ops soit réellement :

1. enregistrée dans PostgreSQL ;
2. chargée par Spring Boot ;
3. transformée en objets métier ;
4. évaluée par le moteur ;
5. utilisée pour prendre une décision ;
6. persistée dans les tables d'exécution ;
7. visible dans l'historique/audit.

---

# 5. UTILISER LE PROTOTYPE EXISTANT

Avant toute modification frontend :

Lis impérativement :

```text
/info/prototype.jsx
```

Le prototype constitue la référence UX/UI actuelle.

Tu dois conserver :

* sa structure générale ;
* son style ;
* ses conventions visuelles ;
* ses écrans existants ;
* ses concepts métier.

Ajoute les nouvelles fonctionnalités dans le même langage visuel.

Ne crée pas une deuxième interface incohérente.

---

# 6. PARTIE OPS — CONFIGURATION DES RÈGLES MÉTIER

Ajouter une partie permettant aux Ops de gérer les règles métier.

L'interface doit permettre au minimum :

### Liste des règles

Afficher :

* code ;
* nom ;
* description ;
* état activé/désactivé ;
* priorité d'évaluation ;
* type de règle ;
* stop on match ;
* date de création/modification si disponible ;
* indication règle par défaut / règle configurable.

Permettre :

* recherche ;
* filtrage ;
* activation/désactivation ;
* modification ;
* duplication ;
* suppression si autorisée ;
* consultation détaillée.

---

# 7. ÉDITEUR DE RÈGLE MÉTIER

Créer un éditeur permettant de construire une règle sans écrire de code.

Une règle doit pouvoir contenir :

### Informations générales

```text
Code
Nom
Description
Enabled
Evaluation priority
Stop on match
```

### Conditions

Supporter des groupes de conditions.

Exemple :

```text
GROUP 1
    category = AVAILABILITY
    AND confidence >= 0.80

GROUP 2
    matchedSolution = "Solution A"
    OR problemType = "Database unavailable"
```

Le système doit respecter la structure :

```text
Rule
 ├── Condition Group
 │     ├── Condition
 │     ├── Condition
 │     └── ...
 │
 ├── Condition Group
 │     └── ...
 │
 ├── Exception Group
 │     ├── Condition
 │     └── ...
 │
 └── Actions
```

Les groupes doivent pouvoir utiliser `AND` / `OR`.

---

# 8. CONDITIONS DISPONIBLES

Ne limite pas artificiellement les conditions à quelques champs.

Construis une abstraction permettant d'évaluer différents types de données.

Les conditions doivent notamment pouvoir exploiter :

### Alert

* severity ;
* status ;
* impactLevel ;
* source ;
* title ;
* problemId ;
* linkedProblemId ;
* start/end time ;
* entity information ;
* tags ;
* etc.

### LLM analysis

* category ;
* problemType ;
* confidence ;
* matchedSolution ;
* matchedDomain ;
* matchedPole ;
* matchedEntity ;
* probableCause ;
* requiresHumanValidation ;
* fallback ;
* uncertainFields ;
* status.

### Business context

* Solution ;
* Domaine ;
* Pôle ;
* Entité ;
* service type ;
* solution type ;
* PSI ;
* target scope ;
* etc.

### Organisation

* rôle métier ;
* niveau organisationnel ;
* personnes disponibles ;
* primary contact ;
* active/inactive.

### Contexte temporel

Prévoir la possibilité d'ajouter ultérieurement des conditions comme :

* heure ;
* jour ;
* plage horaire ;
* durée depuis réception ;
* durée depuis dernière tentative ;
* etc.

Ne hardcode pas chaque condition directement dans le moteur.

Crée une architecture extensible, par exemple avec une abstraction de type :

```text
ConditionEvaluator
```

ou une architecture équivalente.

---

# 9. OPERATORS

Prévoir différents opérateurs selon le type de donnée :

```text
EQUALS
NOT_EQUALS
CONTAINS
NOT_CONTAINS
STARTS_WITH
ENDS_WITH
GREATER_THAN
GREATER_OR_EQUAL
LESS_THAN
LESS_OR_EQUAL
IN
NOT_IN
IS_NULL
IS_NOT_NULL
```

Pour les listes :

```text
CONTAINS
NOT_CONTAINS
```

Pour les valeurs numériques :

```text
>
>=
<
<=
=
!=
```

Pour les booléens :

```text
TRUE
FALSE
```

L'architecture doit permettre d'ajouter de nouveaux opérateurs sans réécrire tout le moteur.

---

# 10. EXCEPTIONS

Une règle doit pouvoir avoir des exceptions.

Exemple :

```text
IF
    category = AVAILABILITY
    AND confidence >= 0.80

EXCEPT IF
    matchedSolution = "Solution critique spéciale"
```

Les exceptions doivent être évaluées avant de considérer la règle comme applicable.

Respecte le modèle actuel :

```text
rule_condition_group.block_type
```

avec :

```text
CONDITION
EXCEPTION
```

Ne recrée pas une table `rule_exception` séparée sauf si l'implémentation démontre une nécessité réelle.

---

# 11. ACTIONS DU BUSINESS RULE ENGINE

Les actions doivent rester métier.

Exemples possibles :

```text
REQUEST_HUMAN_VALIDATION
SELECT_BUSINESS_CONTEXT
SET_BUSINESS_DECISION
MARK_FALLBACK
SKIP_RULE
STOP_EVALUATION
TRIGGER_ROUTING
```

Attention :

**ne permet pas au Business Rule Engine de modifier arbitrairement le PSI officiel.**

Le PSI officiel vient de :

```text
solution_attribute.psi
```

et ne doit pas être une valeur générée dynamiquement par le LLM ou manipulée comme une priorité calculée.

De même, ne crée pas des actions telles que :

```text
SET_PSI
OVERRIDE_PRIORITY
```

si elles contredisent le modèle métier défini.

---

# 12. ORDRE D'ÉVALUATION

Respecter :

```text
business_rule.rule_evaluation_priority
```

Attention :

ce champ signifie uniquement :

> ordre dans lequel les règles sont évaluées.

Il ne représente PAS :

* PSI ;
* criticité ;
* priorité de l'alerte ;
* priorité de notification.

Si :

```text
rule A priority = 10
rule B priority = 20
```

A est évaluée avant B.

---

# 13. STOP ON MATCH

Respecter :

```text
stop_on_match
```

Si :

```text
rule.matches == true
AND rule.stop_on_match == true
```

alors arrêter l'évaluation des règles suivantes.

Ne crée pas inutilement plusieurs modes globaux complexes si `stop_on_match` suffit au modèle actuel.

---

# 14. RÈGLES MÉTIER PAR DÉFAUT

Le système doit disposer d'un ensemble de règles par défaut représentant le comportement normal de V0.

Ces règles doivent être identifiables comme :

```text
DEFAULT
```

ou via une architecture équivalente.

Elles doivent être initialisées automatiquement lors de l'installation du système.

Important :

**ne mets pas uniquement leur comportement en dur dans Java.**

Leur définition doit être suffisamment persistante pour que l'application sache quelles sont les règles normales.

Si tu considères qu'une partie extrêmement technique doit rester dans le code, justifie cette décision dans le code/documentation.

---

# 15. CONFIGURATION DES RÈGLES DE ROUTAGE

Créer une interface Ops séparée pour :

```text
Routing Policies
```

Ne pas mélanger cette interface avec :

```text
Business Rules
```

Une politique de routage possède :

```text
code
name
description
enabled
priority
steps[]
```

---

# 16. ROUTING POLICY EDITOR

L'Ops doit pouvoir construire une séquence :

```text
Step 1
Step 2
Step 3
Step 4
...
```

Chaque étape doit contenir :

```text
step_order
action_type
target_role
target_unit_type
channel
delay_after_seconds
```

Les actions doivent être basées sur le modèle actuel :

```text
VOICE_CALL
VOICE_RETRY
NEXT_PERSON
SMS
EMAIL
NOTIFY_SUPERVISOR
```

Ne suppose pas que toutes les actions sont déjà implémentées techniquement.

Si SMS/Email ne sont pas encore exécutables en V0, la configuration peut exister mais l'exécution doit respecter l'état réel de l'application.

---

# 17. TARGET ROLE

Le routage doit utiliser exclusivement les rôles métier existants :

```text
admin_role_type
```

Ne crée pas de nouveaux rôles arbitraires dans le frontend.

Exemples :

```text
TAM
DBA
INFRA_ADMIN
MANAGER
...
```

Le rôle doit être résolu à un niveau organisationnel :

```text
SOLUTION
DOMAIN
POLE
ENTITY
```

Donc :

```text
target_role
+
target_unit_type
```

déterminent la cible.

Exemple :

```text
target_role = TAM
target_unit_type = SOLUTION
```

signifie :

> rechercher les personnes ayant le rôle TAM associées à la Solution concernée.

---

# 18. RÉSOLUTION DES PERSONNES

Le routage ne doit jamais recevoir directement une liste statique de personnes depuis la règle.

Il doit résoudre dynamiquement les personnes depuis PostgreSQL.

Exemple :

```text
Selected Solution
      ↓
organizational_unit
      ↓
unit_admin_assignment
      ↓
role
      ↓
person
      ↓
member_availability
```

Ensuite seulement :

```text
Person candidates
      ↓
ordering
      ↓
routing step
      ↓
notification
```

Prévoir clairement dans le code une abstraction du genre :

```text
PersonResolver
```

ou équivalent.

Elle doit pouvoir déterminer :

* personnes correspondant au rôle ;
* niveau organisationnel ;
* active/inactive ;
* disponibilité ;
* primary contact ;
* ordre éventuel.

---

# 19. ROUTAGE PAR DÉFAUT

Le comportement normal doit être représenté par une politique de routage par défaut.

Exemple conceptuel :

```text
Default Routing Policy

Step 1
    VOICE_CALL
    TAM
    SOLUTION
    AUTO
    delay = X

Step 2
    VOICE_RETRY
    TAM
    SOLUTION
    AUTO
    delay = Y

Step 3
    NEXT_PERSON
    TAM
    SOLUTION
    delay = Z

Step 4
    NOTIFY_SUPERVISOR
    MANAGER
    DOMAIN
```

Les valeurs exactes ne doivent pas être inventées si elles ne sont pas déjà définies dans le projet.

Si une valeur manque, rends-la configurable plutôt que de créer arbitrairement une règle métier.

---

# 20. ROUTAGE CONFIGURABLE VS DEFAULT

Le moteur doit suivre cette logique :

```text
Find applicable routing policies
        |
        v
Existe-t-il une politique configurable correspondante ?
        |
       YES
        |
        v
Utiliser la politique configurable
        |
       NO
        |
        v
Utiliser la politique par défaut
```

Une politique configurable ne doit pas casser le comportement normal si elle :

* est désactivée ;
* est invalide ;
* ne correspond pas ;
* ne peut pas être résolue.

Prévoir un fallback propre.

---

# 21. INTÉGRATION AVEC LE RÉSULTAT LLM

Le flux réel doit être :

```text
Dynatrace Alert
       ↓
Alert persistence
       ↓
Retrieve business candidates
       ↓
LLM classification
       ↓
ClassificationResponseValidator
       ↓
alert_llm_analysis
       ↓
Business Rule Engine
       ↓
Business decision
       ↓
Routing Engine
       ↓
Routing Policy
       ↓
PersonResolver
       ↓
Routing Steps
       ↓
Notification
```

Le résultat du LLM doit être une **entrée du moteur**, pas la décision finale.

Utiliser notamment :

```text
alert_llm_analysis.confidence
alert_llm_analysis.category
alert_llm_analysis.problem_type
alert_llm_analysis.matched_solution
alert_llm_analysis.matched_domain
alert_llm_analysis.matched_pole
alert_llm_analysis.matched_entity
alert_llm_analysis.resolved_psi
alert_llm_analysis.requires_human_validation
alert_llm_analysis.status
```

---

# 22. IMPORTANT — RESOLVED PSI

Le champ :

```text
resolved_psi
```

dans `alert_llm_analysis` ne doit jamais être considéré comme une valeur fournie par le LLM.

Il doit être résolu depuis :

```text
solution_attribute.psi
```

après résolution de la Solution.

Donc :

```text
LLM matchedSolution
        ↓
resolve organizational_unit
        ↓
solution_attribute
        ↓
psi
```

Le LLM ne décide pas du PSI.

---

# 23. FALLBACKS

Prévoir explicitement les cas :

### LLM indisponible

```text
LLM unavailable
→ fallback métier
```

### JSON invalide

```text
Classification fallback
→ règles métier fallback
```

### confidence faible

```text
LOW_CONFIDENCE
→ règles correspondantes
```

### Solution inconnue

```text
invalid candidate
→ fallback
```

### aucune règle configurable

```text
default business rule
```

### aucune politique de routage configurable

```text
default routing policy
```

### aucune personne trouvée

Prévoir un comportement explicite :

```text
supervisor escalation
```

ou autre comportement défini par la configuration.

Ne jamais laisser le système arriver à un état silencieux.

---

# 24. PERSISTENCE POSTGRESQL

Le modèle actuel contient déjà :

```text
business_rule
rule_condition_group
rule_condition
rule_action
rule_execution
routing_policy
routing_step
routing_execution
routing_history
routing_response
```

Utilise ce modèle comme base.

Mais tu es autorisé à le modifier si l'implémentation réelle révèle un problème.

Par exemple :

* ajouter une colonne ;
* ajouter une contrainte ;
* ajouter un index ;
* modifier une relation ;
* ajouter un discriminant ;
* modifier un type ;
* ajouter une table si réellement nécessaire.

**Ne conserve pas une conception uniquement parce qu'elle existe actuellement.**

Si une modification du MLD est nécessaire pour obtenir un système propre, effectue-la.

---

# 25. DEFAULT RULES EN BASE

Décide intelligemment comment représenter les règles par défaut.

Une possibilité est :

```text
business_rule
```

contient également les règles par défaut avec un attribut permettant de les distinguer.

Par exemple :

```text
rule_origin = DEFAULT
rule_origin = CONFIGURED
```

ou une autre solution techniquement meilleure.

Même logique pour :

```text
routing_policy
```

L'objectif est que le moteur puisse savoir :

```text
DEFAULT
```

vs

```text
CONFIGURED
```

sans dupliquer toute la logique.

Si tu choisis une autre architecture, justifie-la.

---

# 26. SEED / INITIALISATION

Les règles par défaut doivent être installées automatiquement.

Étudier le mécanisme approprié au projet actuel :

* migration SQL ;
* Flyway ;
* data initializer ;
* autre mécanisme déjà utilisé.

Ne crée pas un mécanisme parallèle inutile.

L'installation d'une nouvelle instance doit permettre d'obtenir immédiatement :

```text
Default Business Rules
+
Default Routing Policies
```

sans configuration manuelle obligatoire.

---

# 27. SPRING BOOT — ARCHITECTURE

Respecter autant que possible l'architecture actuelle du projet :

```text
domain
application
infrastructure
```

ou l'organisation déjà présente.

Séparer clairement :

```text
BusinessRuleEngine
RoutingEngine
ConditionEvaluator
ActionExecutor
RoutingPolicySelector
PersonResolver
```

Éviter un énorme service du type :

```text
AlertService
```

qui ferait tout.

---

# 28. BUSINESS RULE ENGINE

Créer une API métier claire, par exemple conceptuellement :

```java
BusinessDecision evaluate(BusinessRuleContext context);
```

Le contexte doit contenir suffisamment d'informations pour évaluer les règles sans que chaque condition fasse directement des requêtes SQL.

Construire d'abord un contexte métier :

```text
Alert
LLM Analysis
Resolved Solution
Domain
Pole
Entity
PSI
People information if required
...
```

Puis évaluer les règles.

Éviter autant que possible :

```text
ConditionEvaluator → repository
ConditionEvaluator → repository
ConditionEvaluator → repository
```

pour chaque condition.

Préparer le contexte en amont.

---

# 29. ROUTING ENGINE

Créer une API métier indépendante, par exemple conceptuellement :

```java
RoutingDecision buildRoutingDecision(RoutingContext context);
```

Puis :

```text
RoutingPolicySelector
       ↓
RoutingStepExecutor
       ↓
PersonResolver
       ↓
Notification service
```

Le moteur de routage ne doit pas connaître les détails internes du LLM.

Il doit recevoir le résultat métier dont il a besoin.

---

# 30. EXECUTION ET HISTORIQUE

Chaque évaluation réelle doit être traçable.

Utiliser :

```text
rule_execution
```

pour savoir :

* quelle règle ;
* quelle alerte ;
* quel résultat LLM ;
* matched ou non ;
* durée ;
* date.

Utiliser :

```text
routing_execution
routing_history
routing_response
```

pour suivre :

* politique sélectionnée ;
* étape courante ;
* personne ciblée ;
* action ;
* réponse ;
* progression.

Ne pas supprimer cette traçabilité simplement pour réduire le nombre de tables.

Ces tables ont une fonction différente des tables de configuration.

---

# 31. AUDIT

Conserver :

```text
audit_log
audit_log_detail
```

pour les actions importantes.

L'audit n'est pas un doublon de `rule_execution`.

Différence :

```text
rule_execution
=
trace technique de l'évaluation d'une règle
```

alors que :

```text
audit_log
=
traçabilité fonctionnelle/sécurité de l'action effectuée
```

Ne fusionne pas ces concepts sans démontrer que l'un peut complètement remplacer l'autre.

---

# 32. FRONTEND

Dans `/info/prototype.jsx`, ajouter les écrans nécessaires.

Créer au minimum :

```text
Ops
 ├── Business Rules
 │     ├── Rules list
 │     ├── Create rule
 │     ├── Edit rule
 │     └── Rule details
 │
 └── Routing Policies
       ├── Policies list
       ├── Create policy
       ├── Edit policy
       └── Policy details
```

Pour les conditions :

```text
[Field]
[Operator]
[Value]
```

avec :

```text
+ Add condition
+ Add group
+ Add exception
```

Pour les actions :

```text
+ Add action
```

Pour le routage :

```text
Step 1
Step 2
Step 3
```

avec possibilité :

* d'ajouter ;
* supprimer ;
* réordonner ;
* modifier ;
* activer/désactiver.

---

# 33. VALIDATION FRONTEND

Empêcher autant que possible :

* une condition vide ;
* une valeur incompatible ;
* un rôle inexistant ;
* un niveau organisationnel invalide ;
* une étape sans action ;
* un délai négatif ;
* deux étapes avec le même ordre si cela est interdit ;
* une politique sans étape ;
* une règle sans condition si le modèle l'interdit.

Mais la validation frontend ne remplace jamais la validation backend.

---

# 34. VALIDATION BACKEND

Le backend doit considérer les données venant du frontend comme non fiables.

Valider :

* enums ;
* opérateurs ;
* types ;
* rôles ;
* niveaux ;
* actions ;
* délais ;
* références ;
* cohérence des groupes ;
* cohérence des exceptions ;
* permissions Ops.

---

# 35. API REST

Créer les endpoints nécessaires pour :

### Business Rules

```text
GET    /api/business-rules
GET    /api/business-rules/{id}
POST   /api/business-rules
PUT    /api/business-rules/{id}
DELETE /api/business-rules/{id}
PATCH  /api/business-rules/{id}/enabled
```

### Routing Policies

```text
GET    /api/routing-policies
GET    /api/routing-policies/{id}
POST   /api/routing-policies
PUT    /api/routing-policies/{id}
DELETE /api/routing-policies/{id}
PATCH  /api/routing-policies/{id}/enabled
```

Adapte les URLs aux conventions déjà utilisées dans le projet.

---

# 36. TESTER LE MOTEUR

Ne te limite surtout pas aux tests CRUD.

Créer des tests unitaires pour :

### Business Rules

```text
condition true
condition false
AND
OR
exception
multiple groups
priority
stop_on_match
fallback
default rule
configured rule
```

### Routing

```text
policy selection
default policy
configured policy
role resolution
unit resolution
person ordering
availability
step progression
timeout
accepted
rejected
supervisor escalation
```

### Integration

Tester un scénario complet :

```text
Alert
→ LLM result
→ business rules
→ business decision
→ routing policy
→ person resolution
→ routing execution
```

---

# 37. SCÉNARIOS CONCRETS À IMPLÉMENTER

Créer au moins quelques scénarios réalistes.

### Scénario 1 — comportement par défaut

```text
Alert
→ LLM confidence élevée
→ Solution trouvée
→ aucune règle configurable correspondante
→ règle métier par défaut
→ politique de routage par défaut
→ TAM de la Solution
→ notification
```

### Scénario 2 — règle configurable

```text
Alert
→ LLM result
→ règle configurable correspondante
→ comportement configuré
→ routage correspondant
```

### Scénario 3 — faible confidence

```text
LLM confidence < threshold
→ LOW_CONFIDENCE
→ règle correspondante
→ validation humaine / fallback
```

### Scénario 4 — aucune personne

```text
Routing
→ aucun TAM disponible
→ étape suivante
ou
→ supervisor
```

### Scénario 5 — timeout

```text
VOICE_CALL
→ TIMEOUT
→ delay
→ next step
```

---

# 38. NE PAS INVENTER LES RÈGLES MÉTIER

Tu dois être créatif sur **l'architecture et l'UX**, mais ne dois pas inventer comme vérité métier des règles qui n'ont jamais été définies.

Lorsqu'une règle métier exacte manque :

* rendre le comportement configurable ;
* créer une règle par défaut raisonnable uniquement si elle est explicitement nécessaire au fonctionnement ;
* documenter l'hypothèse ;
* éviter de coder silencieusement une décision métier arbitraire.

---

# 39. SI LE MODÈLE ACTUEL EST INSUFFISANT

Tu as explicitement l'autorisation de modifier :

```text
Java
SQL
schema.sql
MLD
entities
DTO
repositories
services
controllers
frontend
prototype.jsx
tests
```

si nécessaire.

Mais avant de modifier une partie importante :

1. analyser l'architecture actuelle ;
2. identifier le problème ;
3. choisir la solution la plus cohérente ;
4. appliquer la modification ;
5. vérifier les impacts sur les autres modules.

Ne fais pas de modifications massives sans raison.

---

# 40. COMPATIBILITÉ AVEC LE MODÈLE V2 ACTUEL

Respecter notamment :

```text
organizational_unit
solution_attribute
person
admin_role_type
unit_admin_assignment
member_availability

alert
alert_entity
alert_comment

alert_llm_analysis

business_rule
rule_condition_group
rule_condition
rule_action
rule_execution

routing_policy
routing_step
routing_execution
routing_history
routing_response

notification
notification_template
notification_recipient
notification_attempt
voice_message

audit_log
audit_log_detail
system_event
application_metric
```

Les règles doivent utiliser ce modèle plutôt que créer des tables parallèles ayant la même responsabilité.

---

# 41. CE QUE JE VEUX À LA FIN

À la fin de cette tâche, je veux un système réellement fonctionnel et pas seulement un prototype visuel.

Je dois pouvoir :

```text
1. ouvrir l'interface Ops ;
2. créer une règle métier ;
3. définir ses conditions ;
4. définir ses exceptions ;
5. définir ses actions ;
6. sauvegarder ;
7. retrouver cette règle depuis PostgreSQL ;
8. l'activer ;
9. recevoir une alerte ;
10. obtenir le résultat LLM ;
11. faire évaluer la règle ;
12. observer que la règle correspond ;
13. observer la décision produite ;
14. sélectionner une politique de routage ;
15. résoudre les personnes ;
16. exécuter les étapes ;
17. enregistrer l'exécution ;
18. voir l'historique.
```

Et également :

```text
aucune règle configurable correspondante
        ↓
règle par défaut
        ↓
routage par défaut
```

doit fonctionner réellement.

---

# 42. LIVRABLES ATTENDUS

À la fin, fournis :

### Code

Tous les fichiers Java nécessaires.

### Database

Les modifications nécessaires de :

```text
schema.sql
migrations
seed/default data
```

### Frontend

Les modifications de :

```text
/info/prototype.jsx
```

et les composants réels correspondants s'ils existent.

### Tests

Tests unitaires + intégration des moteurs.

### Documentation

Ajouter une courte documentation expliquant :

```text
Business Rule Engine
Routing Engine
Default Rules
Configured Rules
Rule precedence
Fallback
LLM integration
Person resolution
Persistence
```

---

# 43. TRÈS IMPORTANT — AVANT DE CODER

Ne commence pas immédiatement par créer des fichiers.

Commence par analyser :

```text
1. architecture Spring Boot actuelle
2. module classification
3. module alert
4. module business rules existant
5. module routing existant
6. notification
7. repositories
8. entities
9. schema.sql
10. prototype.jsx
```

Ensuite identifie :

```text
- ce qui existe déjà ;
- ce qui est incomplet ;
- ce qui est contradictoire ;
- ce qui doit être réutilisé ;
- ce qui doit être modifié ;
- ce qui manque ;
- les impacts BDD ;
- les impacts frontend ;
- les impacts métier.
```

Puis implémente.

**Ne recrée pas ce qui existe déjà.**

**Ne crée pas une architecture parallèle.**

Réutilise au maximum les composants existants lorsqu'ils correspondent au besoin.

---

# 44. RÈGLE DE CONCEPTION FINALE

Le résultat doit respecter cette séparation :

```text
                    ┌──────────────────────┐
                    │       LLM            │
                    │ Classification only  │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │ BUSINESS RULE ENGINE │
                    │                      │
                    │ Default rules        │
                    │ Configured rules     │
                    │ Conditions           │
                    │ Exceptions           │
                    │ Business actions     │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │    ROUTING ENGINE    │
                    │                      │
                    │ Default policy       │
                    │ Configured policy    │
                    │ Person resolution    │
                    │ Steps                │
                    │ Delays               │
                    │ Escalation           │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │ NOTIFICATION ENGINE  │
                    │ Voice / SMS / Email  │
                    └──────────────────────┘
```

La règle la plus importante est :

> **Le LLM produit une analyse. Le Business Rule Engine transforme cette analyse en décision métier. Le Routing Engine transforme cette décision en stratégie d'action et de destinataires. Le Notification Engine exécute concrètement la communication.**

Les règles par défaut représentent le **chemin normal de fonctionnement**.

Les règles configurées par les Ops représentent des **branches alternatives conditionnelles** capables de modifier le comportement normal lorsqu'elles correspondent.

Le système doit rester déterministe, traçable, configurable et extensible.

---

### Instruction finale à Cursor

**Ne considère pas cette demande comme une simple tâche CRUD.**

Il s'agit de l'implémentation du **cœur décisionnel de l'application**.

Si, pendant l'analyse, tu constates que le modèle actuel ne permet pas proprement de réaliser cette architecture, **arrête-toi sur le point concerné, explique le problème et propose une modification concrète**, puis applique-la si elle est nécessaire à une implémentation cohérente.

Ne privilégie pas la conservation artificielle du code actuel au détriment de la cohérence du système.

L'objectif final est d'obtenir un système réellement exécutable de bout en bout, où la configuration réalisée par un Ops n'est pas simplement affichée dans l'interface mais **change effectivement le comportement du moteur Java et est persistée dans PostgreSQL**.
