| <h1><b>Brina: Text-Editor-with-AI</b></h1> | &nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp;|
|---|---|
| Редактор документов с возможностью записи текста с помощью голоса. Поддерживает чат gpt, который сможет ответить на вопрос, а также подкорректировать текст, записанный голосовым вводом, по запросу из выпадающего пользователю списка. Также есть возможность создавать папки с документами и давать права доступа определенным людям для просмотра документов. | <img src="https://github.com/B-E-D-A/Text-Editor-with-AI/assets/112130616/cd3542ad-55fd-424e-954c-76d0d1f34327" alt="small-icon" width="200" height="200"> |


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
| <img src="https://github.com/B-E-D-A/Text-Editor-with-AI/assets/112130616/cf516f22-2380-4fa0-a820-64e6cd8436b6" alt="sign-in" width="447" height="319"> | <img src="https://github.com/B-E-D-A/Text-Editor-with-AI/assets/112130616/9c08785a-ee25-4cfd-9d7a-912c0035ba38" alt="sign-up" width="447" height="319"> |
| <img src="https://github.com/B-E-D-A/Brina/assets/112130616/20e3bca7-df05-4a18-99da-c8084cd39b63" alt="main-window" width="447" height="319"> | <img src="https://github.com/B-E-D-A/Brina/assets/112130616/2ac05c89-b82c-457a-abb2-976cba79266b" alt="editor" width="447" height="319"> |
| <img src="https://github.com/B-E-D-A/Brina/assets/112130616/e5c83c54-df33-45d8-9eec-db1d37ee05a5" alt="main-window" width="447" height="465"> | <img src="https://github.com/B-E-D-A/Brina/assets/112130616/f4cedf9c-457b-4875-b5a0-924faba15ae5" alt="editor" width="447" height="300"> |
