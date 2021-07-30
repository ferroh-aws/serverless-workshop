# serverless-workshop

---

En este workshop construiremos una aplicación para tokenizar información de un archivo cargado en S3, sustituiremos el campo de cuenta por un token y enviaremos el resultado a otro bucket de S3. Adicionalmente replicaremos la información de las cuentas a través de DynamoDB streams.

## Estructura

- DataReplicationFunction - Contiene la función lambda codificada en Java que realizará la sincronización de la tabla de DynamoDB al lago de datos.
- DataUploadFunction - Contiene la función lambda codificada en Java que se encargará de sustituir la información de la cuenta por un Token generado con UUID.
- events - Contiene eventos en formato JSON que podemos utilizar para realizar pruebas.
- template.yaml - Plantilla de AWS Serverless Application Model [AWS SAM](https://aws.amazon.com/serverless/sam/) con la definición de los recursos utilizados en este workshop.

## Comandos

Para construir el proyecto utilizaremos el siguiente comando:
```commandline
sam build
```
Para desplegar el proyecto la primera vez utilizaremos:
```commandline
sam deploy --guided --capabilities CAPABILITY_NAMED_IAM
```
Para despliegues posteriores ya que tenemos configurado el proyecto podemos utilizar:
```commandline
sam deploy --capabilities CAPABILITY_NAMED_IAM
```

## Recursos

Para definir cada recurso utilizamos la plantilla definida en el archivo ```template.yaml```, esta plantilla utiliza [Amazon CloudFormation](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/Welcome.html) para definir recursos en conjunto con [AWS SAM](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/what-is-sam.html). En este archivo crearemos los siguientes recursos en la sección ```resources```:

- FailuresDLQ - Cola del servicio Amazon Simple Queue Service que nos servirá como Dead Letter Queue para los registros que no puedan ser procesados.
  ```yaml
    FailuresDLQ:
      Type: AWS::SQS::Queue
      Properties:
        QueueName: WorkshopDLQ
  ```
- DataUploadFunctionRole - Definiremos un rol de AWS Identity & Access Management con los permisos mínimos necesarios para ejecutar la función.
  ```yaml
    DataUploadFunctionRole:
      Type: AWS::IAM::Role
      Properties:
        AssumeRolePolicyDocument:
          Version: 2012-10-17
          Statement:
            - Action:
                - sts:AssumeRole
              Effect: Allow
              Principal:
                Service:
                  - lambda.amazonaws.com
        Description: Role used by the DataUploadFunction.
        ManagedPolicyArns:
          - arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
        Path: /
        Policies:
          - PolicyName: root
            PolicyDocument:
              Version: 2012-10-17
              Statement:
                - Action:
                    - s3:GetObject
                  Effect: Allow
                  Resource:
                    - !Sub 'arn:aws:s3:::serverless-upload-${AWS::AccountId}/*'
                - Action:
                    - s3:PutObject
                  Effect: Allow
                  Resource:
                    - !Sub 'arn:aws:s3:::serverless-destination-${AWS::AccountId}/*'
                - Action:
                    - dynamodb:BatchGetItem
                    - dynamodb:BatchWriteItem
                  Effect: Allow
                  Resource:
                    - !Sub 'arn:aws:dynamodb:${AWS::Region}:${AWS::AccountId}:table/${TokensTableName}'
        RoleName: DataUploadFunctionRole
  ```
- DataReplicationFunctionRole - De igual forma necesitamos un rol con los permisos mínimos necesarios para la función de replicación de datos.
  ```yaml
    DataReplicationFunctionRole:
      Type: AWS::IAM::Role
      Properties:
        AssumeRolePolicyDocument:
          Version: 2012-10-17
          Statement:
            - Action:
                - sts:AssumeRole
              Effect: Allow
              Principal:
                Service:
                  - lambda.amazonaws.com
        Description: Role used by the DataReplicationFunction.
        ManagedPolicyArns:
          - arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
        Path: /
        Policies:
          - PolicyName: root
            PolicyDocument:
              Version: 2012-10-17
              Statement:
                - Action:
                    - s3:PutObject
                  Effect: Allow
                  Resource:
                    - !Sub 'arn:aws:s3:::serverless-destination-${AWS::AccountId}/*'
                - Action:
                    - sqs:SendMessage
                  Effect: Allow
                  Resource:
                    - !GetAtt FailuresDLQ.Arn
                - Action:
                    - dynamodb:DescribeStream
                    - dynamodb:GetRecords
                    - dynamodb:GetShardIterator
                  Effect: Allow
                  Resource:
                    - !Sub 'arn:aws:dynamodb:${AWS::Region}:${AWS::AccountId}:table/${TokensTableName}/stream/*'
                - Action:
                    - dynamodb:ListStreams
                  Effect: Allow
                  Resource: '*'
        RoleName: DataReplicationFunctionRole
  ```
- DataUploadKey - Para encriptar el bucket de subida utilizaremos una llave simétrica de KMS. A continuación la definiremos.
  ```yaml
    DataUploadKey:
      Type: AWS::KMS::Key
      Properties:
        Description: Symmetric key for S3 bucket encryption.
        Enabled: true
        EnableKeyRotation: true
        PendingWindowInDays: 10
        KeyPolicy:
          Version: 2012-10-17
          Id: key-default
          Statement:
            - Sid: Account Root
              Effect: Allow
              Principal:
                AWS:
                  Fn::Join:
                    - ''
                    - - 'arn:aws:iam::'
                      - Ref: AWS::AccountId
                      - ':root'
              Action:
                - kms:*
              Resource: '*'
            - Sid: Administrators Access
              Effect: Allow
              Principal:
                AWS:
                  Fn::Join:
                    - ''
                    - - 'arn:aws:iam::'
                      - Ref: AWS::AccountId
                      - ':user/'
                      - Ref: KeyAdminUser
              Action:
                - kms:Create*
                - kms:Describe*
                - kms:Enable*
                - kms:List*
                - kms:Put*
                - kms:Update*
                - kms:Revoke*
                - kms:Disable*
                - kms:Get*
                - kms:Delete*
                - kms:TagResource
                - kms:UntagResource
                - kms:ScheduleKeyDeletion
                - kms:CancelKeyDeletion
              Resource: '*'
            - Sid: Allow use of the key
              Effect: Allow
              Principal:
                AWS:
                  - !GetAtt DataUploadFunctionRole.Arn
              Action:
                - kms:Encrypt
                - kms:Decrypt
                - kms:ReEncrypt*
                - kms:GenerateDataKey*
                - kms:DescribeKey
              Resource: '*'
  ```
- DataDestinationKey - Ahora crearemos otra llave para el bucket destino.
  ```yaml
    DataDestinationKey:
      Type: AWS::KMS::Key
      Properties:
        Description: Symmetric key used for the S3 bucket encryption.
        Enabled: true
        EnableKeyRotation: true
        PendingWindowInDays: 10
        KeyPolicy:
          Version: 2012-10-17
          Id: key-default
          Statement:
            - Sid: Account Root
              Effect: Allow
              Principal:
                AWS:
                  Fn::Join:
                    - ''
                    - - 'arn:aws:iam::'
                      - Ref: AWS::AccountId
                      - ':root'
              Action: 'kms:*'
              Resource: '*'
            - Sid: Administrators Access
              Effect: Allow
              Principal:
                AWS:
                  Fn::Join:
                    - ''
                    - - 'arn:aws:iam::'
                      - Ref: AWS::AccountId
                      - ':user/'
                      - Ref: KeyAdminUser
              Action:
                - kms:Create*
                - kms:Describe*
                - kms:Enable*
                - kms:List*
                - kms:Put*
                - kms:Update*
                - kms:Revoke*
                - kms:Disable*
                - kms:Get*
                - kms:Delete*
                - kms:TagResource
                - kms:UntagResource
                - kms:ScheduleKeyDeletion
                - kms:CancelKeyDeletion
              Resource: '*'
            - Sid: Allow use of the key
              Effect: Allow
              Principal:
                AWS:
                  - !GetAtt DataReplicationFunctionRole.Arn
                  - !GetAtt DataUploadFunctionRole.Arn
              Action:
                - kms:Encrypt
                - kms:Decrypt
                - kms:ReEncrypt*
                - kms:GenerateDataKey*
                - kms:DescribeKey
              Resource: '*'
  ```
- TokenVaultKey - Otra llave para la tabla de DynamoDB.
  ```yaml
    TokenVaultKey:
      Type: AWS::KMS::Key
      Properties:
        Description: Symmetric key used to encrypt data at rest in DynamoDB.
        Enabled: true
        EnableKeyRotation: true
        PendingWindowInDays: 10
        KeyPolicy:
          Version: 2012-10-17
          Id: key-default
          Statement:
            - Sid: Account Root
              Effect: Allow
              Principal:
                AWS:
                  Fn::Join:
                    - ''
                    - - 'arn:aws:iam::'
                      - Ref: AWS::AccountId
                      - ':root'
              Action: 'kms:*'
              Resource: '*'
            - Sid: Administrators Access
              Effect: Allow
              Principal:
                AWS:
                  Fn::Join:
                    - ''
                    - - 'arn:aws:iam::'
                      - Ref: AWS::AccountId
                      - ':user/'
                      - Ref: KeyAdminUser
              Action:
                - kms:Create*
                - kms:Describe*
                - kms:Enable*
                - kms:List*
                - kms:Put*
                - kms:Update*
                - kms:Revoke*
                - kms:Disable*
                - kms:Get*
                - kms:Delete*
                - kms:TagResource
                - kms:UntagResource
                - kms:ScheduleKeyDeletion
                - kms:CancelKeyDeletion
              Resource: '*'
            - Sid: Allow use of the key
              Effect: Allow
              Principal:
                AWS:
                  - !GetAtt DataReplicationFunctionRole.Arn
                  - !GetAtt DataUploadFunctionRole.Arn
              Action:
                - kms:Encrypt
                - kms:Decrypt
                - kms:ReEncrypt*
                - kms:GenerateDataKey*
                - kms:DescribeKey
              Resource: '*'
  ```
- SensibleDataKey - Otra llave para los datos sensibles, en este caso los tokens replicados.
  ```yaml
    SensibleDataKey:
      Type: AWS::KMS::Key
      Properties:
        Description: Symmetric key used to encrypt data at rest in DynamoDB.
        Enabled: true
        EnableKeyRotation: true
        PendingWindowInDays: 10
        KeyPolicy:
          Version: 2012-10-17
          Id: key-default
          Statement:
            - Sid: Account Root
              Effect: Allow
              Principal:
                AWS:
                  Fn::Join:
                    - ''
                    - - 'arn:aws:iam::'
                      - Ref: AWS::AccountId
                      - ':root'
              Action: 'kms:*'
              Resource: '*'
            - Sid: Administrators Access
              Effect: Allow
              Principal:
                AWS:
                  Fn::Join:
                    - ''
                    - - 'arn:aws:iam::'
                      - Ref: AWS::AccountId
                      - ':user/'
                      - Ref: KeyAdminUser
              Action:
                - kms:Create*
                - kms:Describe*
                - kms:Enable*
                - kms:List*
                - kms:Put*
                - kms:Update*
                - kms:Revoke*
                - kms:Disable*
                - kms:Get*
                - kms:Delete*
                - kms:TagResource
                - kms:UntagResource
                - kms:ScheduleKeyDeletion
                - kms:CancelKeyDeletion
              Resource: '*'
            - Sid: Allow use of the key
              Effect: Allow
              Principal:
                AWS:
                  - !GetAtt DataReplicationFunctionRole.Arn
                  - !GetAtt DataUploadFunctionRole.Arn
              Action:
                - kms:Encrypt
                - kms:Decrypt
                - kms:ReEncrypt*
                - kms:GenerateDataKey*
                - kms:DescribeKey
              Resource: '*'
  ```
- DataUploadBucket - Creamos el bucket donde se subiran los archivos a procesar.
  ```yaml
    DataUploadBucket:
      Type: AWS::S3::Bucket
      DeletionPolicy: Delete
      Properties:
        AccessControl: Private
        BucketEncryption:
          ServerSideEncryptionConfiguration:
            - BucketKeyEnabled: true
              ServerSideEncryptionByDefault:
                SSEAlgorithm: 'aws:kms'
                KMSMasterKeyID: !GetAtt DataUploadKey.Arn
        BucketName: !Sub 'serverless-upload-${AWS::AccountId}'
  ```
- DataDestinationBucket - Creamos el bucket donde se colocaran los archivos ya procesados.
  ```yaml
    DataDestinationBucket:
      Type: AWS::S3::Bucket
      DeletionPolicy: Delete
      Properties:
        AccessControl: Private
        BucketEncryption:
          ServerSideEncryptionConfiguration:
            - BucketKeyEnabled: true
              ServerSideEncryptionByDefault:
                SSEAlgorithm: 'aws:kms'
                KMSMasterKeyID: !GetAtt DataDestinationKey.Arn
        BucketName: !Sub 'serverless-destination-${AWS::AccountId}'
  ```
- TokenVaultTable - Creamos la tabla de DynamoDB que utilizaremos como bóveda de tokens.
  ```yaml
    TokenVaultTable:
      Type: AWS::DynamoDB::Table
      Properties:
        AttributeDefinitions:
          - AttributeName: account
            AttributeType: S
        BillingMode: PAY_PER_REQUEST
        KeySchema:
          - AttributeName: account
            KeyType: HASH
        SSESpecification:
          KMSMasterKeyId: !Ref TokenVaultKey
          SSEEnabled: true
          SSEType: KMS
        StreamSpecification:
          StreamViewType: NEW_IMAGE
        TableName: !Ref TokensTableName
  ```
- DataReplicationFunction - Creamos la definición de la lambda para replicar información.
  ```yaml
    DataReplicationFunction:
      Type: AWS::Serverless::Function
      Properties:
        CodeUri: DataReplicationFunction/
        Environment:
          Variables:
            TOKEN_VAULT_TABLE: !Ref TokensTableName
            DESTINATION_BUCKET: !Ref DataDestinationBucket
        Events:
          TokensStream:
            Type: DynamoDB
            Properties:
              BatchSize: 100
              BisectBatchOnFunctionError: true
              Enabled: true
              DestinationConfig:
                OnFailure:
                  Destination: !GetAtt FailuresDLQ.Arn
              StartingPosition: TRIM_HORIZON
              Stream: !GetAtt TokenVaultTable.StreamArn
        Handler: aws.workshop.App::requestHandler
        MemorySize: 256
        Role: !GetAtt DataReplicationFunctionRole.Arn
        Runtime: java11
  ```
- DataUploadFunction - Finalmente definimos la lambda para procesar la información cargada al bucket.
  ```yaml
    DataUploadFunction:
      Type: AWS::Serverless::Function
      Properties:
        CodeUri: DataUploadFunction/
        Environment:
          Variables:
            TOKEN_VAULT_TABLE: !Ref TokensTableName
            DESTINATION_BUCKET: !Ref DataDestinationBucket
        Events:
          Upload:
            Type: S3
            Properties:
              Bucket: !Ref DataUploadBucket
              Events: s3:ObjectCreated:*
              Filter:
                S3Key:
                  Rules:
                    - Name: suffix
                      Value: .csv
        Handler: aws.workshop.App::requestHandler
        MemorySize: 512
        Role: !GetAtt DataUploadFunctionRole.Arn
        Runtime: java11
  ```
## Solución

El archivo ```template.yaml``` debe ser similar o idéntico al archivo solution.yaml.

## Funciones

Para poder desplegar nuestra solución es necesario implementar las funciones Lambda:
1. DataReplicationFunction - Esta función es la encargada de replicar los tokens a un bucket de S3 que simulará la capa cruda de un lago de datos.
2. DataUploadFunction - Esta función es la encargada de procesar los archivos cargados al bucket de cargas de S3, su principal función es tokenizar el número de cuenta.