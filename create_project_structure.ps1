# Script PowerShell pour créer la structure de projet Spring Boot

param(
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"

# Fonction pour créer un dossier s'il n'existe pas
function New-DirectoryIfNotExists {
    param([string]$Path)
    if (-not (Test-Path -Path $Path -PathType Container)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
        Write-Host "Dossier cree: $Path" -ForegroundColor Green
    } else {
        Write-Host "Existe deja: $Path" -ForegroundColor Yellow
    }
}

# Fonction pour créer un fichier s'il n'existe pas
function New-FileIfNotExists {
    param([string]$Path)
    if (-not (Test-Path -Path $Path -PathType Leaf)) {
        New-Item -ItemType File -Path $Path -Force | Out-Null
        Write-Host "Fichier cree: $Path" -ForegroundColor Cyan
    } else {
        Write-Host "Existe deja: $Path" -ForegroundColor Yellow
    }
}

# Chemin de base
$basePath = Join-Path $ProjectRoot "src/main/java/FST/MST_RSI/PFA"
$testBasePath = Join-Path $ProjectRoot "src/test/java/FST/MST_RSI/PFA"

Write-Host "=== CREATION DE LA STRUCTURE DU PROJET ===" -ForegroundColor Magenta
Write-Host "Chemin de base: $basePath" -ForegroundColor Magenta

# 1. Création de la structure principale
Write-Host "`n=== STRUCTURE PRINCIPALE ===" -ForegroundColor Blue

# Dossiers principaux
$mainFolders = @(
    "common/domain/vo",
    "common/exception",
    "common/event",
    "common/infrastructure/llm",
    "common/util",
    "common/config",
    "security/domain",
    "security/infrastructure/persistence",
    "security/config",
    "pipeline/config",
    "alerting/domain/model",
    "alerting/domain/service",
    "alerting/domain/event",
    "alerting/domain/port",
    "alerting/application/usecase",
    "alerting/application/dto",
    "alerting/application/mapper",
    "alerting/infrastructure/persistence",
    "alerting/api/rest",
    "alerting/api/webhook",
    "classification/domain/model",
    "classification/domain/event",
    "classification/domain/port",
    "classification/application/usecase",
    "classification/infrastructure/litellm",
    "rulesengine/domain/model",
    "rulesengine/domain/event",
    "rulesengine/domain/port",
    "rulesengine/domain/service",
    "rulesengine/application/usecase",
    "rulesengine/application/dto",
    "rulesengine/application/mapper",
    "rulesengine/infrastructure/persistence",
    "rulesengine/api/rest",
    "routingengine/domain/model",
    "routingengine/domain/event",
    "routingengine/domain/port",
    "routingengine/domain/service/strategy",
    "routingengine/domain/service/escalation",
    "routingengine/application/usecase",
    "routingengine/application/dto",
    "routingengine/application/mapper",
    "routingengine/infrastructure/persistence",
    "routingengine/api/rest",
    "voicemessage/domain/model",
    "voicemessage/domain/port",
    "voicemessage/domain/event",
    "voicemessage/application/usecase",
    "voicemessage/infrastructure/llm",
    "voicemessage/infrastructure/tts",
    "notification/domain/model",
    "notification/domain/event",
    "notification/domain/port",
    "notification/application/usecase",
    "notification/infrastructure/registry",
    "notification/infrastructure/email",
    "notification/infrastructure/sms",
    "notification/infrastructure/voip",
    "notification/infrastructure/ticketing",
    "directory/domain/model",
    "directory/domain/port",
    "directory/application/usecase",
    "directory/infrastructure/persistence",
    "audit/domain/model",
    "audit/application/usecase",
    "audit/infrastructure/persistence",
    "audit/api/rest",
    "dashboard/application/query",
    "dashboard/infrastructure/persistence",
    "dashboard/api/rest",
    "messaging/config",
    "messaging/consumer",
    "messaging/error",
    "scheduling"
)

foreach ($folder in $mainFolders) {
    $fullPath = Join-Path $basePath $folder
    New-DirectoryIfNotExists -Path $fullPath
}

# 2. Création des fichiers Java
Write-Host "`n=== FICHIERS SOURCE ===" -ForegroundColor Blue

# Common
$commonFiles = @(
    "common/exception/BusinessException.java",
    "common/exception/TechnicalException.java",
    "common/exception/ResourceNotFoundException.java",
    "common/exception/GlobalExceptionHandler.java",
    "common/event/DomainEvent.java",
    "common/infrastructure/llm/LiteLlmHttpClient.java",
    "common/config/JacksonConfig.java",
    "common/config/AsyncConfig.java",
    "common/config/OpenApiConfig.java",
    "common/config/CorsConfig.java",
    "common/config/CacheConfig.java"
)

foreach ($file in $commonFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# Security
$securityFiles = @(
    "security/domain/Role.java",
    "security/infrastructure/JwtAuthenticationConverter.java",
    "security/infrastructure/persistence/UserRoleMappingRepository.java",
    "security/config/SecurityConfig.java"
)

foreach ($file in $securityFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# Pipeline
$pipelineFiles = @(
    "pipeline/AlertProcessingOrchestrator.java",
    "pipeline/config/PipelineEventListenersConfig.java"
)

foreach ($file in $pipelineFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# Alerting
$alertingFiles = @(
    "alerting/domain/model/Alert.java",
    "alerting/domain/model/AlertTimelineEntry.java",
    "alerting/domain/model/NotificationState.java",
    "alerting/domain/service/AlertPayloadValidator.java",
    "alerting/domain/event/AlertReceivedEvent.java",
    "alerting/domain/port/AlertRepositoryPort.java",
    "alerting/application/usecase/IngestAlertUseCase.java",
    "alerting/application/usecase/GetAlertDetailUseCase.java",
    "alerting/application/usecase/ListRecentAlertsUseCase.java",
    "alerting/application/usecase/ListAlertHistoryUseCase.java",
    "alerting/application/dto/AlertDto.java",
    "alerting/application/dto/AlertSummaryDto.java",
    "alerting/application/dto/RawPayloadDto.java",
    "alerting/application/mapper/AlertMapper.java",
    "alerting/infrastructure/persistence/AlertEntity.java",
    "alerting/infrastructure/persistence/SpringDataAlertRepository.java",
    "alerting/infrastructure/persistence/AlertRepositoryAdapter.java",
    "alerting/api/rest/AlertController.java",
    "alerting/api/webhook/DynatraceWebhookController.java"
)

foreach ($file in $alertingFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# Classification
$classificationFiles = @(
    "classification/domain/model/ClassificationResult.java",
    "classification/domain/model/Category.java",
    "classification/domain/model/ConfidenceScore.java",
    "classification/domain/event/AlertClassifiedEvent.java",
    "classification/domain/port/AlertClassifierPort.java",
    "classification/application/usecase/ClassifyAlertUseCase.java",
    "classification/infrastructure/litellm/LiteLlmClassifierAdapter.java",
    "classification/infrastructure/litellm/LiteLlmProperties.java",
    "classification/infrastructure/litellm/PromptTemplateProvider.java"
)

foreach ($file in $classificationFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# RulesEngine
$rulesEngineFiles = @(
    "rulesengine/domain/model/Rule.java",
    "rulesengine/domain/model/ConditionGroup.java",
    "rulesengine/domain/model/Condition.java",
    "rulesengine/domain/model/RuleException.java",
    "rulesengine/domain/model/RuleResult.java",
    "rulesengine/domain/model/RuleStatus.java",
    "rulesengine/domain/event/BusinessResultDeterminedEvent.java",
    "rulesengine/domain/event/RuleChangedEvent.java",
    "rulesengine/domain/port/RuleRepositoryPort.java",
    "rulesengine/domain/service/RuleEvaluator.java",
    "rulesengine/domain/service/ConditionSpecification.java",
    "rulesengine/domain/service/RuleIndex.java",
    "rulesengine/application/usecase/EvaluateRulesUseCase.java",
    "rulesengine/application/usecase/CreateRuleUseCase.java",
    "rulesengine/application/usecase/PublishRuleUseCase.java",
    "rulesengine/application/usecase/TestRuleAgainstProfileUseCase.java",
    "rulesengine/application/usecase/ImportRulesUseCase.java",
    "rulesengine/application/usecase/ExportRulesUseCase.java",
    "rulesengine/application/dto/RuleDto.java",
    "rulesengine/application/mapper/RuleMapper.java",
    "rulesengine/infrastructure/persistence/RuleEntity.java",
    "rulesengine/infrastructure/persistence/RuleHistoryEntity.java",
    "rulesengine/infrastructure/persistence/JpaRuleRepositoryAdapter.java",
    "rulesengine/infrastructure/persistence/CachedRuleRepositoryAdapter.java",
    "rulesengine/api/rest/RuleController.java"
)

foreach ($file in $rulesEngineFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# RoutingEngine
$routingEngineFiles = @(
    "routingengine/domain/model/RoutingPolicy.java",
    "routingengine/domain/model/RoutingCondition.java",
    "routingengine/domain/model/Channel.java",
    "routingengine/domain/model/EscalationPolicy.java",
    "routingengine/domain/model/VoipScenarioStep.java",
    "routingengine/domain/model/RoutingDecision.java",
    "routingengine/domain/event/RoutingDecisionMadeEvent.java",
    "routingengine/domain/event/RoutingPolicyChangedEvent.java",
    "routingengine/domain/port/RoutingPolicyRepositoryPort.java",
    "routingengine/domain/port/OrganizationalUnitPort.java",
    "routingengine/domain/service/RoutingPolicyResolver.java",
    "routingengine/domain/service/strategy/MemberSelectionStrategy.java",
    "routingengine/domain/service/strategy/FixedOrderStrategy.java",
    "routingengine/domain/service/strategy/FirstAvailableStrategy.java",
    "routingengine/domain/service/escalation/EscalationChainResolver.java",
    "routingengine/application/usecase/ResolveRoutingDecisionUseCase.java",
    "routingengine/application/usecase/SimulateRoutingUseCase.java",
    "routingengine/application/dto/RoutingPolicyDto.java",
    "routingengine/application/mapper/RoutingPolicyMapper.java",
    "routingengine/infrastructure/persistence/RoutingPolicyEntity.java",
    "routingengine/infrastructure/persistence/MaintenanceWindowEntity.java",
    "routingengine/infrastructure/persistence/JpaRoutingPolicyRepositoryAdapter.java",
    "routingengine/infrastructure/persistence/CachedRoutingPolicyRepositoryAdapter.java",
    "routingengine/api/rest/RoutingPolicyController.java"
)

foreach ($file in $routingEngineFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# VoiceMessage
$voiceMessageFiles = @(
    "voicemessage/domain/model/VoiceScript.java",
    "voicemessage/domain/port/VoiceScriptGeneratorPort.java",
    "voicemessage/domain/port/TextToSpeechPort.java",
    "voicemessage/domain/event/VoiceMessageGeneratedEvent.java",
    "voicemessage/application/usecase/GenerateVoiceMessageUseCase.java",
    "voicemessage/infrastructure/llm/LiteLlmVoiceScriptAdapter.java",
    "voicemessage/infrastructure/tts/OpenAiTtsAdapter.java",
    "voicemessage/infrastructure/tts/ElevenLabsTtsAdapter.java",
    "voicemessage/infrastructure/tts/AzureSpeechTtsAdapter.java",
    "voicemessage/infrastructure/tts/PiperTtsAdapter.java",
    "voicemessage/infrastructure/tts/CoquiTtsAdapter.java",
    "voicemessage/infrastructure/tts/LocalModelTtsAdapter.java"
)

foreach ($file in $voiceMessageFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# Notification
$notificationFiles = @(
    "notification/domain/model/NotificationAttempt.java",
    "notification/domain/model/NotificationStatus.java",
    "notification/domain/event/NotificationSentEvent.java",
    "notification/domain/event/NotificationFailedEvent.java",
    "notification/domain/port/NotificationSenderPort.java",
    "notification/application/usecase/ExecuteNotificationWorkflowUseCase.java",
    "notification/application/usecase/RetryNotificationUseCase.java",
    "notification/application/usecase/ReassignNotificationUseCase.java",
    "notification/infrastructure/registry/NotificationSenderRegistry.java",
    "notification/infrastructure/email/EmailNotificationSenderAdapter.java",
    "notification/infrastructure/sms/KafkaSmsNotificationSenderAdapter.java",
    "notification/infrastructure/voip/VoipGatewayAdapter.java",
    "notification/infrastructure/ticketing/TicketingClientAdapter.java"
)

foreach ($file in $notificationFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# Directory
$directoryFiles = @(
    "directory/domain/model/OrganizationalUnit.java",
    "directory/domain/model/UnitAdminAssignment.java",
    "directory/domain/model/Member.java",
    "directory/domain/model/Availability.java",
    "directory/domain/model/ChannelCapability.java",
    "directory/domain/port/OrganizationalUnitRepositoryPort.java",
    "directory/application/usecase/GetUnitAdminGroupUseCase.java",
    "directory/application/usecase/GetMemberCommunicationStatsUseCase.java",
    "directory/infrastructure/persistence/OrganizationalUnitEntity.java",
    "directory/infrastructure/persistence/UnitAdminAssignmentEntity.java",
    "directory/infrastructure/persistence/MemberEntity.java"
)

foreach ($file in $directoryFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# Audit
$auditFiles = @(
    "audit/domain/model/SystemLogEntry.java",
    "audit/application/usecase/RecordAuditEventUseCase.java",
    "audit/infrastructure/persistence/AuditRepository.java",
    "audit/api/rest/SystemLogController.java"
)

foreach ($file in $auditFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# Dashboard
$dashboardFiles = @(
    "dashboard/application/query/GetDashboardKpisQuery.java",
    "dashboard/application/query/GetCommunicationStatsQuery.java",
    "dashboard/infrastructure/persistence/DashboardProjectionRepository.java",
    "dashboard/api/rest/DashboardController.java"
)

foreach ($file in $dashboardFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# Messaging
$messagingFiles = @(
    "messaging/config/KafkaTopicsConfig.java",
    "messaging/consumer/SmsNotificationConsumer.java",
    "messaging/consumer/DeadLetterConsumer.java",
    "messaging/error/KafkaErrorHandlerConfig.java"
)

foreach ($file in $messagingFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# Scheduling
$schedulingFiles = @(
    "scheduling/EscalationSweepScheduledTask.java",
    "scheduling/StatsRefreshScheduledTask.java",
    "scheduling/MaintenanceWindowSweepScheduledTask.java"
)

foreach ($file in $schedulingFiles) {
    $fullPath = Join-Path $basePath $file
    New-FileIfNotExists -Path $fullPath
}

# 3. Création de la structure de test
Write-Host "`n=== STRUCTURE DE TEST ===" -ForegroundColor Blue

$testFolders = @(
    "common",
    "security",
    "pipeline",
    "alerting",
    "classification",
    "rulesengine",
    "routingengine",
    "voicemessage",
    "notification",
    "directory",
    "audit",
    "dashboard",
    "messaging",
    "scheduling",
    "integration",
    "testsupport"
)

foreach ($folder in $testFolders) {
    $fullPath = Join-Path $testBasePath $folder
    New-DirectoryIfNotExists -Path $fullPath
}

# 4. Création du fichier application principal (sans BOM)
$mainAppFile = Join-Path $basePath "AlertOpsApplication.java"
if (-not (Test-Path $mainAppFile)) {
    $content = @"
package FST.MST_RSI.PFA;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AlertOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertOpsApplication.class, args);
    }
}
"@
    # Sauvegarder sans BOM
    [System.IO.File]::WriteAllText($mainAppFile, $content, [System.Text.UTF8Encoding]::new($false))
    Write-Host "Fichier cree: $mainAppFile" -ForegroundColor Cyan
} else {
    Write-Host "Existe deja: $mainAppFile" -ForegroundColor Yellow
}

# 5. Création d'un fichier de configuration application.properties si nécessaire
$configPath = Join-Path $ProjectRoot "src/main/resources"
New-DirectoryIfNotExists -Path $configPath

$propertiesFile = Join-Path $configPath "application.properties"
if (-not (Test-Path $propertiesFile)) {
    $propertiesContent = @"
# Spring Boot Configuration
spring.application.name=alert-ops
spring.datasource.url=jdbc:postgresql://localhost:5432/alertops
spring.datasource.username=alertops
spring.datasource.password=alertops
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092

# Logging
logging.level.FST.MST_RSI.PFA=DEBUG

# Application specific
app.voice.tts.provider=openai
app.litellm.url=http://localhost:8000
"@
    Set-Content -Path $propertiesFile -Value $propertiesContent -Encoding UTF8
    Write-Host "Fichier cree: $propertiesFile" -ForegroundColor Cyan
} else {
    Write-Host "Existe deja: $propertiesFile" -ForegroundColor Yellow
}

Write-Host "`n=== STRUCTURE CREEE AVEC SUCCES ===" -ForegroundColor Green