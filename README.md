# GastoZen — Controle de Gastos Pessoais

Aplicativo Android nativo para controle de finanças pessoais com foco em praticidade: lança despesas escaneando QR codes de notas fiscais, compartilhando comprovantes PIX ou imagens/PDFs direto do WhatsApp ou qualquer app.

---

## Funcionalidades

### Lançamentos manuais
- Registro de despesas (débito, crédito parcelado) e receitas
- Forma de pagamento: Dinheiro, Cartão Débito, Cartão Crédito, PIX, Outros
- Vinculação a conta e categoria
- Observações, data personalizada, foto de comprovante
- Lançamentos recorrentes (gerados automaticamente todo mês via WorkManager)

### Escaneamento de Nota Fiscal Eletrônica (NF-e)
- Lê o QR code da NF-e com a câmera
- Busca os dados da nota via XML da SEFAZ (ou fallback HTML)
- Cria **um único lançamento** com o valor total da compra
- Armazena todos os produtos na tabela `produtos_comprados` com nome, NCM, quantidade, valor unitário e desconto rastreado
- Após o scan, produtos sem categoria abrem a **tela de classificação**: o usuário atribui uma categoria para cada item e a regra é salva — próximos scans do mesmo produto são classificados automaticamente

### Comprovante PIX via compartilhamento de texto
- Compartilhe o texto de um comprovante PIX de qualquer banco (Nubank, Inter, Itaú, BB, Bradesco, C6, Mercado Pago, PicPay etc.)
- O app detecta valor, direção (enviado/recebido) e nome da outra parte
- Abre a tela de novo lançamento já pré-preenchida

### Comprovante via imagem ou PDF
- Compartilhe uma imagem (print de comprovante, foto, encaminhamento do WhatsApp) ou um PDF
- O app usa **ML Kit Text Recognition** para extrair o texto via OCR
- Tenta identificar valor, tipo (entrada/saída) e nome automaticamente
- Abre tela de confirmação onde o usuário define:
  - **Entrada** (receita) ou **Saída** (despesa)
  - Categoria (chips selecionáveis, apenas para saídas)
  - Descrição e valor (pré-preenchidos se reconhecidos)

### Dashboard
- Resumo mensal: total de receitas, despesas e saldo
- **Dashboard de categorias**: barra colorida proporcional + linhas clicáveis por categoria
- Clicando em uma categoria → lista dos lançamentos e produtos NF-e daquele mês na categoria
- Navegação mensal (mês anterior / próximo)

### Produtos comprados
- Lista mensal de todos os itens de NF-e registrados
- Mostra quantidade, valor unitário e categoria de cada produto

### Metas por categoria
- Define limite mensal de gasto por categoria
- Barra de progresso mostrando quanto já foi usado do limite

### Histórico
- Lista completa de lançamentos com filtros

### Configurações
- Gerenciamento de contas e categorias
- Gerenciamento de recorrentes
- Backup e restauração do banco de dados

---

## Tecnologias

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navegação | Navigation Compose |
| Arquitetura | MVVM + Repository + Use Cases |
| Banco de dados | Room (SQLite) |
| Câmera | CameraX |
| OCR / Barcode | ML Kit (Text Recognition + Barcode Scanning) |
| Tarefas em background | WorkManager |
| Rede | OkHttp + Jsoup |
| Imagens | Coil |
| Gráficos | Vico |
| Widget | Glance |

---

## Arquitetura

```
app/
└── src/main/java/com/gastozen/
    ├── data/
    │   ├── db/          # AppDatabase, DAOs, Converters, Migrations
    │   ├── model/       # Entities (Lancamento, Categoria, Conta, ProdutoComprado…)
    │   └── repository/  # Repositórios que expõem Flow para a camada de domínio
    ├── domain/
    │   └── usecase/     # CriarLancamentoUseCase, ClassificarProdutoUseCase…
    ├── ui/
    │   ├── comprovante/ # ReceberComprovanteScreen — OCR de imagem/PDF
    │   ├── dashboard/   # DashboardScreen, GastosCategoriaScreen
    │   ├── historico/
    │   ├── lancamento/  # LancamentoScreen, QrCodeScreen, ClassificarProdutosScreen
    │   ├── metas/
    │   ├── configuracoes/
    │   ├── produtos/    # ProdutosCompradosScreen
    │   ├── categorias/
    │   ├── theme/
    │   └── widget/
    ├── util/
    │   ├── NfeXmlParser.kt      # Parser do XML da NF-e
    │   ├── NfeHtmlParser.kt     # Fallback HTML da SEFAZ
    │   ├── PixReceiptParser.kt  # Parser de texto de comprovante PIX
    │   ├── OcrHelper.kt         # OCR via ML Kit (imagem e PDF)
    │   ├── PendingPix.kt        # Singleton: PIX pendente entre Activity e ViewModel
    │   ├── PendingComprovante.kt# Singleton: URI de imagem/PDF pendente
    │   └── DateUtils.kt
    ├── worker/
    │   └── RecorrenteWorker.kt  # Geração automática de lançamentos recorrentes
    ├── MainActivity.kt
    └── MainNavGraph.kt
```

### Banco de dados (versão 2)

```
lancamentos          — registro financeiro principal
produtos_comprados   — itens de NF-e (N para 1 lancamento)
categorias           — categorias de gasto
contas               — contas/carteiras
metas                — limites mensais por categoria
recorrentes          — templates de lançamento mensal
regras_categoria     — mapeamento nome de produto → categoria (aprendizado)
```

---

## Fluxo de compartilhamento

```
Usuário compartilha no Android
        │
        ├─ text/plain  → PixReceiptParser → PendingPix → LancamentoScreen (pré-preenchido)
        │
        ├─ image/*     → PendingComprovante → OcrHelper (ML Kit) → PixReceiptParser
        │                └─ ReceberComprovanteScreen (entrada/saída + categoria + salvar)
        │
        └─ application/pdf → PendingComprovante → OcrHelper (PdfRenderer + ML Kit)
                             └─ ReceberComprovanteScreen
```

---

## Build

Pré-requisitos: JDK 17, Android SDK 34.

```bash
./gradlew assembleDebug
# APK gerado em: app/build/outputs/apk/debug/app-debug.apk
```

O projeto **não requer Android Studio** — todo o desenvolvimento é feito via Gradle na linha de comando.

---

## Banco de dados — Migrações

| Versão | Mudanças |
|--------|----------|
| 1 | Schema inicial |
| 2 | Adicionado `tipoPagamento`, `desconto`, `nfeCnpj` em `lancamentos`; criada tabela `produtos_comprados`; inseridas 6 novas categorias padrão |
