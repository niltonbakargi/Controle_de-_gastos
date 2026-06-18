package com.gastozen.ui.lancamento

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastozen.data.model.TipoPagamento
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScreen(
    viewModel: QrCodeViewModel,
    onBack: () -> Unit,
    onNfeDetected: () -> Unit,
    onClassificarProdutos: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var scanning by remember { mutableStateOf(true) }
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraPermissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is QrCodeUiState.NfeCarregada -> {
                scanning = false
                if (s.semCategoria.isEmpty()) {
                    onNfeDetected()
                } else {
                    onClassificarProdutos(s.lancamentoId)
                }
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ler QR Code NF-e") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!cameraPermissionGranted) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Permissão de câmera necessária.", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Conceder permissão")
                    }
                }
            } else if (scanning) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onQrDetected = { url ->
                        if (scanning) {
                            scanning = false
                            viewModel.processarUrl(url)
                        }
                    }
                )
            }

            when (val state = uiState) {
                is QrCodeUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    Text(
                        "Consultando SEFAZ...",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                is QrCodeUiState.Error -> {
                    Card(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { scanning = true; viewModel.resetar() }) {
                                    Text("Tentar novamente")
                                }
                                OutlinedButton(onClick = {
                                    val report = buildString {
                                        appendLine("=== GastoZen - Erro QR Code ===")
                                        appendLine("Erro: ${state.message}")
                                        if (state.url.isNotBlank()) appendLine("URL: ${state.url}")
                                        appendLine("Data: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale("pt","BR")).format(java.util.Date())}")
                                    }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "GastoZen - Erro no QR Code")
                                        putExtra(Intent.EXTRA_TEXT, report)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Reportar erro"))
                                }) { Text("Reportar") }
                            }
                        }
                    }
                }

                is QrCodeUiState.NfeSumario -> {
                    NfeSumarioCard(
                        nota = state.nota,
                        onConfirmar = { tipoPagamento ->
                            viewModel.confirmarNota(state.nota, tipoPagamento, null)
                        },
                        onCancelar = { scanning = true; viewModel.resetar() },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun NfeSumarioCard(
    nota: com.gastozen.util.NotaFiscal,
    onConfirmar: (TipoPagamento) -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tipoPagamento by remember { mutableStateOf(TipoPagamento.DINHEIRO) }

    Card(
        modifier = modifier.padding(16.dp).fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Nota lida! ${nota.produtos.size} produto(s)",
                style = MaterialTheme.typography.titleMedium
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total NF-e", style = MaterialTheme.typography.bodyMedium)
                Text("R$ ${"%.2f".format(nota.valorTotal)}", style = MaterialTheme.typography.bodyMedium)
            }
            if (nota.desconto > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Desconto", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("- R$ ${"%.2f".format(nota.desconto)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Valor pago", style = MaterialTheme.typography.bodyMedium)
                    Text("R$ ${"%.2f".format(maxOf(nota.valorTotal - nota.desconto, 0.0))}")
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Forma de pagamento", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(TipoPagamento.values()) { tp ->
                    FilterChip(
                        selected = tipoPagamento == tp,
                        onClick = { tipoPagamento = tp },
                        label = { Text(tp.label()) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onConfirmar(tipoPagamento) },
                    modifier = Modifier.weight(1f)
                ) { Text("Salvar") }
                OutlinedButton(
                    onClick = onCancelar,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancelar") }
            }
        }
    }
}

private fun TipoPagamento.label() = when (this) {
    TipoPagamento.DINHEIRO       -> "Dinheiro"
    TipoPagamento.CARTAO_DEBITO  -> "Débito"
    TipoPagamento.CARTAO_CREDITO -> "Crédito"
    TipoPagamento.PIX            -> "PIX"
    TipoPagamento.OUTROS         -> "Outros"
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    @androidx.camera.core.ExperimentalGetImage
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage, imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                    ?.rawValue
                                    ?.takeIf { it.startsWith("http") }
                                    ?.let { onQrDetected(it) }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}
