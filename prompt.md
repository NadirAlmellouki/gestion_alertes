Tu dois maintenant finaliser l'import du référentiel Excel AlertOps dans PostgreSQL.

IMPORTANT :
Cette tâche ne consiste PAS simplement à lire l'Excel et à insérer toutes les lignes.
Le fichier Excel contient des données imparfaites et sa structure ne correspond pas directement 1:1 aux tables PostgreSQL.

L'objectif est d'obtenir une base PostgreSQL cohérente avec le MLD et le modèle métier, en nettoyant et validant les données AVANT insertion.

========================================================
1. DOCUMENTS ET SOURCES DE VÉRITÉ
========================================================

Avant toute modification, lis attentivement :

/info/Master Prompt V2
/info/roadmap.md
/info/MLD_Global.txt
/info/schema.sql
/info/referentiel-applicatif-complet-2026-08-04.xlsx

Lis également le vrai payload Dynatrace disponible dans /info, notamment :

/info/JSON_Alert_Dynatrace.json

et les autres fichiers pertinents présents dans /info.

Tu dois également analyser le code actuellement implémenté, les migrations Flyway, les entités JPA, les repositories et la configuration de l'import déjà développée.

IMPORTANT :
- Le MLD et schema.sql représentent le modèle PostgreSQL de référence actuellement retenu.
- schema.sql a déjà été exécuté avec succès dans le conteneur PostgreSQL.
- Ne crée PAS de nouvelles tables simplement parce qu'une donnée Excel ne trouve pas immédiatement de destination.
- Ne modifie PAS le MLD pour forcer l'Excel à entrer dans le modèle.
- Ne suis PAS l'architecture de manière dogmatique si tu identifies une incohérence conceptuelle.
- La priorité est la cohérence métier et relationnelle.
- Ne jamais inventer une donnée absente du fichier Excel.

========================================================
2. OBJECTIF
========================================================

Importer les données pertinentes du fichier :

/info/referentiel-applicatif-complet-2026-08-04.xlsx

dans les tables PostgreSQL existantes.

Le fichier contient notamment des feuilles relatives à :

- Solutions
- Entités
- Pôles
- Domaines
- Personnes

Tu dois analyser réellement les colonnes et les valeurs présentes dans chaque feuille avant de décider du mapping.

Ne pars PAS uniquement du nom des colonnes.
Examine les données réelles.

========================================================
3. HIÉRARCHIE MÉTIER
========================================================

La hiérarchie confirmée par les données Excel est :

Solution
    ↓
Domaine
    ↓
Pôle
    ↓
Entité

Il ne faut PAS utiliser une hiérarchie Application → Pôle → Domaine.

Les relations doivent être créées dans PostgreSQL avec les identifiants réels des lignes déjà insérées.

Exemple :

Entité
  ↓
Pôle
  ↓
Domaine
  ↓
Solution

Chaque niveau doit être résolu vers son parent réel.

Ne crée jamais plusieurs fois la même unité organisationnelle simplement parce qu'elle apparaît dans plusieurs lignes Excel.

========================================================
4. TABLES POSTGRESQL À UTILISER
========================================================

Consulte schema.sql et le MLD pour déterminer précisément les colonnes et contraintes.

Les principales tables du référentiel sont notamment :

- organizational_unit
- solution_attribute
- person
- admin_role_type
- unit_admin_assignment
- member_availability

Tu dois également vérifier les contraintes UNIQUE, FOREIGN KEY, NOT NULL et les types de données avant l'import.

Ne suppose pas que toutes les colonnes Excel doivent être importées.

Pour chaque colonne Excel, détermine explicitement :

Excel column
    → PostgreSQL table
    → PostgreSQL column
    → règle de transformation
    → règle de nettoyage
    → comportement si donnée invalide

Produis d'abord cette matrice de mapping dans ton rapport avant d'exécuter l'import.

========================================================
5. NETTOYAGE DES PERSONNES — TRÈS IMPORTANT
========================================================

La feuille Personnes contient un problème important.

Il existe environ 200 lignes où la colonne censée contenir l'e-mail contient en réalité seulement un nom complet ou une valeur qui ne constitue PAS un e-mail exploitable.

Il existe également des lignes sans :

- nom
- prénom
- responsabilité/fonction

Ces lignes ne doivent PAS être insérées aveuglément dans la table person.

Règle impérative :

Une personne ne doit être créée que si elle possède suffisamment d'informations fiables pour représenter une vraie personne du référentiel.

Au minimum, l'identité doit être exploitable.

Si une ligne contient uniquement :

"Jean Dupont"

dans une colonne e-mail, ce n'est PAS un e-mail.

Ne transforme jamais cette valeur arbitrairement en :

jean.dupont@example.com

ou toute autre donnée inventée.

Si une ligne contient uniquement une valeur qui ne permet pas d'identifier correctement une personne, elle doit être :

- rejetée ou ignorée
- comptabilisée
- expliquée dans le rapport d'import

Les lignes incomplètes ne doivent donc PAS polluer la table person.

========================================================
6. NORMALISATION DES PERSONNES
========================================================

Avant insertion :

- trim des espaces
- normalisation des e-mails en lowercase
- détection des e-mails invalides
- déduplication par e-mail lorsque l'e-mail est fiable
- suppression des lignes totalement vides
- distinction entre nom réel et e-mail
- ne jamais fabriquer nom/prénom à partir d'un e-mail si les règles métier ne le justifient pas
- ne jamais inventer une responsabilité

Si plusieurs lignes Excel représentent la même personne, une seule personne doit être créée.

Les affectations de cette personne doivent ensuite référencer le même person.id.

========================================================
7. SOLUTIONS
========================================================

Pour les solutions :

- analyser les lignes actives et inactives
- conserver la distinction active/inactive si le modèle PostgreSQL le permet
- ne pas utiliser une solution inactive comme candidat de classification active
- nettoyer les valeurs texte
- éviter les doublons
- résoudre correctement Domaine → Pôle → Entité
- insérer les attributs métier dans solution_attribute

Les informations comme :

- PSI
- Type
- Rôle
- Popularité
- Tenant
- Type service
- Active

doivent être mappées vers les colonnes appropriées du modèle PostgreSQL selon schema.sql.

IMPORTANT :

Le PSI est une donnée métier fixe du référentiel.

Il ne doit PAS être calculé par Gemini.

========================================================
8. ADMINISTRATEURS ET RESPONSABILITÉS
========================================================

Les différentes colonnes de rôles présentes dans la feuille Solutions ne doivent pas être transformées en texte libre dans la table solution.

Elles doivent être reliées au modèle :

person
    ↓
unit_admin_assignment
    ↓
admin_role_type
    ↓
organizational_unit

Utilise les personnes existantes lorsqu'elles peuvent être identifiées de manière fiable.

Si une personne référencée dans une colonne de responsabilité n'existe pas encore dans person mais peut être identifiée de manière fiable par son e-mail, elle peut être créée selon les règles de nettoyage.

Si l'identité est ambiguë ou invalide :

NE PAS inventer la personne.

Journaliser le problème et rejeter l'affectation concernée.

========================================================
9. RESPONSABLES DES ENTITÉS / PÔLES / DOMAINES
========================================================

Analyse également les colonnes Responsable des feuilles :

- Entités
- Pôles
- Domaines

Ces responsables doivent être reliés au modèle relationnel approprié.

Ne crée pas une nouvelle personne pour chaque occurrence.

Utilise une résolution par identité/e-mail lorsque possible.

========================================================
10. DONNÉES INCOHÉRENTES
========================================================

Certaines lignes peuvent avoir :

- domaine absent
- pôle absent
- domaine ne correspondant pas au pôle
- pôle ne correspondant pas à l'entité
- solution sans hiérarchie complète
- personne impossible à identifier
- rôle administratif sans personne valide

Ne force PAS l'insertion.

Une donnée incohérente doit être :

1. détectée
2. expliquée
3. comptabilisée
4. ignorée ou rejetée selon sa gravité

Il est préférable d'avoir 600 solutions correctement insérées que 698 lignes insérées avec des relations fausses.

========================================================
11. IMPORT IDEMPOTENT
========================================================

L'import doit être idempotent.

Si je lance :

mvn spring-boot:run

ou la commande d'import plusieurs fois, je ne dois PAS obtenir :

- doublons de personnes
- doublons d'entités
- doublons de pôles
- doublons de domaines
- doublons de solutions
- doublons d'affectations

L'import doit rechercher les données existantes avant insertion ou utiliser les contraintes/UPSERT appropriés.

Ne supprime PAS les données existantes sans justification.

========================================================
12. ORDRE D'IMPORT
========================================================

Respecte les dépendances relationnelles.

Ordre recommandé :

1. admin_role_type / données de référence nécessaires
2. personnes valides
3. entités
4. pôles
5. domaines
6. solutions
7. solution_attribute
8. unit_admin_assignment
9. member_availability uniquement si les données nécessaires existent réellement

À chaque étape :

- récupérer l'ID PostgreSQL réel
- l'utiliser pour les relations
- ne jamais supposer que l'ID correspond au numéro de ligne Excel

========================================================
13. RAPPORT D'IMPORT
========================================================

L'import doit produire un rapport clair.

Exemple :

Personnes :
- lignes Excel : 488
- insérées : X
- déjà existantes : Y
- rejetées : Z
- raisons principales : ...

Solutions :
- lignes : 698
- actives : 427
- inactives : 271
- insérées : X
- ignorées : Y
- rejetées : Z

Entités :
- trouvées : X
- insérées : Y

Pôles :
- trouvés : X
- insérés : Y

Domaines :
- trouvés : X
- insérés : Y

Affectations :
- créées : X
- rejetées : Y

Le rapport doit également indiquer les erreurs importantes avec suffisamment de contexte pour permettre une correction manuelle du fichier Excel si nécessaire.

========================================================
14. BASE POSTGRESQL RÉELLE
========================================================

Tu as accès à l'environnement de développement et à la configuration PostgreSQL.

Avant d'insérer quoi que ce soit :

- vérifier que PostgreSQL est disponible
- vérifier la base utilisée par Spring Boot
- vérifier le schéma public
- vérifier les tables réellement présentes
- vérifier les contraintes principales

Ne crée PAS de nouvelle base.

Ne recrée PAS les tables.

Ne supprime PAS le schéma.

Ne fais PAS de DROP DATABASE.

L'objectif est d'alimenter la base existante conformément à schema.sql.

========================================================
15. CONFIGURATION DU FICHIER EXCEL
========================================================

Le fichier existe déjà dans :

/info/referentiel-applicatif-complet-2026-08-04.xlsx

Vérifie la propriété :

app.referential.excel-path

Elle doit pointer vers ce fichier.

Si la configuration actuelle est incorrecte, corrige-la.

Si elle est correcte, ne la modifie pas inutilement.

Attention au contexte d'exécution :

le chemin relatif doit être résolu par rapport au répertoire depuis lequel Spring Boot est lancé.

Utiliser de préférence :

./info/referentiel-applicatif-complet-2026-08-04.xlsx

si cela correspond au fonctionnement actuel du projet.

Ne demande PAS à l'utilisateur de déplacer le fichier : il est déjà présent dans /info.

========================================================
16. RÈGLES DE CLASSIFICATION ET RAG
========================================================

Après l'import, le référentiel PostgreSQL doit devenir la source de vérité métier pour la classification.

Le catalogue JSON utilisé actuellement pour le développement doit être considéré comme fallback/test et non comme source de vérité finale.

Le pipeline de classification doit évoluer vers :

Dynatrace payload
      ↓
AlertContextExtractor
      ↓
BusinessContextPort
      ↓
PostgreSQL
      ↓
retrieval Top-K
      ↓
candidats Solutions
      ↓
prompt Gemini enrichi
      ↓
ClassificationResult
      ↓
validation stricte
      ↓
résolution du PSI depuis PostgreSQL

Le LLM ne doit jamais inventer une solution qui n'existe pas parmi les candidats fournis.

Le LLM ne calcule jamais le PSI.

Le LLM ne décide pas du routage.

Le LLM ne reçoit pas les e-mails des administrateurs.

========================================================
17. RAG / RETRIEVAL
========================================================

Analyse le vrai payload Dynatrace dans /info pour déterminer quelles informations sont réellement pertinentes pour le retrieval.

Le retrieval doit pouvoir exploiter notamment, lorsque disponibles :

- problemId
- displayId
- title
- severityLevel
- impactLevel
- status
- affectedEntities
- impactedEntities
- rootCauseEntity
- entityTags
- managementZones
- informations Kubernetes
- evidenceDetails
- impactAnalysis
- linkedProblemInfo

Ne mets PAS tout le JSON brut dans le prompt.

Construis un contexte compact et pertinent.

Le retrieval doit chercher les solutions pertinentes à partir du contexte technique et métier de l'alerte.

Exemple conceptuel :

Alert Dynatrace
      ↓
extraction des signaux utiles
      ↓
recherche PostgreSQL
      ↓
Top-K solutions
      ↓
hiérarchie Domaine/Pôle/Entité
      ↓
prompt Gemini

Le nombre de candidats doit être configurable, avec une valeur raisonnable par défaut, par exemple 5 à 8.

========================================================
18. PROMPT GEMINI
========================================================

Une fois le retrieval PostgreSQL fonctionnel, reconstruis le prompt de classification.

Structure recommandée :

SYSTEM
- rôle du modèle
- contraintes
- interdictions

ALERT
- contexte technique extrait de Dynatrace

CANDIDATES
- uniquement les solutions récupérées depuis PostgreSQL
- informations utiles de solution
- hiérarchie
- description
- type/service
- autres attributs réellement nécessaires

TASK
- sélectionner la meilleure solution parmi les candidats
- fournir une confiance
- expliquer brièvement le choix
- retourner fallback si aucun candidat fiable

OUTPUT
JSON strict

IMPORTANT :

NE PAS demander :

proposedPriority
PSI calculé
routing decision
administrators
phone numbers
notification channel

Le PSI sera récupéré côté application depuis solution_attribute après validation du match.

========================================================
19. VALIDATION
========================================================

Après modification :

- compiler le projet
- démarrer Spring Boot
- vérifier ddl-auto=validate
- vérifier que toutes les tables attendues existent
- vérifier l'import
- vérifier les relations FK
- vérifier l'absence de doublons
- vérifier les contraintes NOT NULL
- vérifier les personnes rejetées
- vérifier les solutions actives
- vérifier que le retrieval PostgreSQL fonctionne
- vérifier que le prompt Gemini contient réellement les candidats récupérés depuis PostgreSQL
- vérifier que le PSI vient de PostgreSQL et non du LLM

Ne te contente PAS de dire que le code est correct.

Exécute réellement les vérifications lorsque cela est possible.

========================================================
20. CONTRAINTES IMPORTANTES
========================================================

NE PAS :

- supprimer la base
- recréer arbitrairement les tables
- modifier schema.sql pour faire entrer l'Excel de force
- inventer des personnes
- inventer des e-mails
- inventer des responsabilités
- insérer les ~200 lignes de personnes manifestement invalides
- envoyer les administrateurs au LLM
- demander au LLM de calculer le PSI
- demander au LLM de décider du routage
- utiliser uniquement le JSON comme source de vérité après migration
- considérer toutes les lignes Excel comme valides par défaut

À la fin, fournis :

1. les fichiers modifiés
2. les changements effectués
3. la matrice Excel → PostgreSQL
4. le nombre de lignes importées par table
5. le nombre de lignes rejetées/ignorées
6. les raisons des rejets
7. les éventuelles données ambiguës nécessitant une décision humaine
8. les tests exécutés et leurs résultats
9. un exemple concret d'une alerte Dynatrace
10. les candidats récupérés depuis PostgreSQL
11. le prompt final réellement envoyé à Gemini
12. le résultat de classification
13. la résolution du PSI depuis PostgreSQL

Ne fais pas de modification destructive sans me l'indiquer explicitement avant de l'exécuter.