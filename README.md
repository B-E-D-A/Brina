<img src="https://github.com/B-E-D-A/Brina/assets/112130616/bae70e83-f563-480b-a828-48284f2076d0" alt="small-icon" width="370" height="70">           ***Text-Editor-with-AI***

### Возможности пользователя:

- Базовый набор функций редактора документов
- Распознавание текста из аудио-записей mp3
- Запись текста голосом
- Использование чата GPT
- Предоставление прав доступа(чтение/редактирование) для определенных людей
- Сохранение документов в разных форматах (pdf, txt, docx)

### Инструкция по установке приложения - Intellij IDEA

- скачайте javafx 17.0.10 -  https://gluonhq.com/products/javafx/, распакуйте zip


- File - Project Structure - Libraries : New Project Library типа Java - в папках находите распакованный zip и выбирайте из него папку lib


- Edit Configurations - Add new configuration - Application:

`Name = Main`,
`module not specified = 19`,
`-cp \<no module\> = Text-Editor-with-AI.main`, 
`Main Class = org.hse.brina.Main`

Modify options —> Add VM options: `--module-path "path\openjfx-17.0.10_windows-x64_bin-sdk\javafx-sdk-17.0.10\lib" --add-modules javafx.controls,javafx.fxml` , где в кавычках надо указать путь к папке lib у скачанной библиотеки javafx

- в папке проекта Brina создайте файл `.env`, скопируйте туда содержимое `.env.example` и допишите недостающую информацию:
  - ID проекта Google Cloud `regal-crowbar-421701`
  - ID приватного ключа (для Google Cloud)
  - Приватный ключ (для Google Cloud)
  - Электронная почта клиента `brina-373@regal-crowbar-421701.iam.gserviceaccount.com`
  - ID клиента `108426607798802531281`
  - URL для сертификата клиента `https://www.googleapis.com/robot/v1/metadata/x509/brina-373%40regal-crowbar-421701.iam.gserviceaccount.com`
  - API key для чата YandexGPT


- запускаем `Connector.java`, после того, как он отработает: `Server.java`, далее `Main.java` (Main, который настоили)

### Текущий вид приложения

| | |
|---|---|
| <img src="https://github.com/B-E-D-A/Brina/assets/112130616/831250e2-b3ab-4e7e-9e2a-c1a4463b9b83" alt="sign-in" width="447" height="319"> | <img src="https://github.com/B-E-D-A/Brina/assets/112130616/215412d3-2256-4dfc-af3c-7229310a1539" alt="sign-up" width="447" height="319"> |
| <img src="https://github.com/B-E-D-A/Brina/assets/112130616/e5c73dd0-f9a9-43e0-b914-8b5a668fe20f" alt="main-window" width="447" height="319"> | <img src="https://github.com/B-E-D-A/Brina/assets/112130616/955f330d-0644-4fed-8d9a-50ac8b66a4d1" alt="editor" width="447" height="319"> |
| <img src="https://github.com/B-E-D-A/Brina/assets/112130616/e5c7f1c3-05a8-434c-9a6b-69f28bafd44f" alt="main-window" width="447" height="465"> | <img src="https://github.com/B-E-D-A/Brina/assets/112130616/5f9e1cf8-dd53-4772-a42b-6c3d71a0bd4c" alt="editor" width="447" height="300"> |
| <img src="https://github.com/B-E-D-A/Brina/assets/112130616/7120bd96-6ce4-4047-b26d-d50ab5dc9c71" alt="main-window" width="447" height="300"> | <img src="https://github.com/B-E-D-A/Brina/assets/112130616/3a407a0d-d1f9-4b14-b685-71fa83c1f6a6" alt="editor" width="447" height="300"> |
