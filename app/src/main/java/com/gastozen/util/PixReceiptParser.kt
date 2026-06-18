package com.gastozen.util

import com.gastozen.data.model.TipoLancamento
import com.gastozen.data.model.TipoPagamento

data class PixRecibo(
    val valor: Double,
    val tipo: TipoLancamento,   // DEBITO = pagamento enviado | RECEITA = recebido
    val descricao: String,      // nome da outra parte ou descrição
    val tipoPagamento: TipoPagamento = TipoPagamento.PIX
)

/**
 * Extrai dados de comprovantes PIX compartilhados como texto pelos principais bancos
 * brasileiros (Nubank, Inter, Itaú, BB, Bradesco, C6, Mercado Pago, PicPay, etc.).
 */
object PixReceiptParser {

    // ── Valor ─────────────────────────────────────────────────────────────────
    // Captura: "R$ 1.500,00" / "R$150,00" / "Valor: 150,00" / "1500.00"
    private val valorPatterns = listOf(
        Regex("""R\$\s*([0-9]{1,3}(?:\.[0-9]{3})*,[0-9]{2})"""),           // R$ 1.500,00
        Regex("""R\$\s*([0-9]+,[0-9]{2})"""),                               // R$ 150,00
        Regex("""(?:valor|value|amount)[^\d]*([0-9]{1,3}(?:\.[0-9]{3})*,[0-9]{2})""", RegexOption.IGNORE_CASE),
        Regex("""(?:valor|value|amount)[^\d]*([0-9]+\.[0-9]{2})\b""",       RegexOption.IGNORE_CASE)
    )

    // ── Direção: DÉBITO (você pagou / enviou) ─────────────────────────────────
    private val debitoKeywords = listOf(
        "você pagou", "voce pagou", "pagamento enviado", "pix enviado",
        "transferência enviada", "transferencia enviada", "enviado com sucesso",
        "debitado", "você transferiu", "voce transferiu", "pagou", "efetuado",
        "pix realizado", "você realizou", "pagamento realizado", "ted enviada",
        "doc enviado", "você debitou"
    )

    // ── Direção: RECEITA (você recebeu) ──────────────────────────────────────
    private val receitaKeywords = listOf(
        "você recebeu", "voce recebeu", "pix recebido", "recebido com sucesso",
        "creditado", "transferência recebida", "transferencia recebida",
        "depósito recebido", "deposito recebido", "crédito recebido",
        "credito recebido", "ted recebida", "doc recebido", "recebimento"
    )

    // ── Nome da outra parte ───────────────────────────────────────────────────
    // Para débito: quem recebeu
    private val receptorPatterns = listOf(
        Regex("""(?:para|favorecido|recebedor|destinat[aá]rio|benefici[aá]rio)[:\s]+([^\n\r]+)""", RegexOption.IGNORE_CASE),
        Regex("""você pagou[^\n]*para\s+([^\n\r]+)""",   RegexOption.IGNORE_CASE),
        Regex("""enviado para\s+([^\n\r]+)""",            RegexOption.IGNORE_CASE),
        Regex("""pix para\s+([^\n\r]+)""",                RegexOption.IGNORE_CASE)
    )

    // Para receita: quem enviou
    private val remetentePatterns = listOf(
        Regex("""(?:de|pagador|remetente|origem|enviado por)[:\s]+([^\n\r]+)""", RegexOption.IGNORE_CASE),
        Regex("""([^\n\r]+)\s+(?:pagou|enviou|transferiu) para você""",          RegexOption.IGNORE_CASE),
        Regex("""recebido de\s+([^\n\r]+)""",                                    RegexOption.IGNORE_CASE)
    )

    /**
     * Analisa o texto do comprovante e retorna um [PixRecibo] ou null se não
     * for possível extrair ao menos o valor.
     */
    fun parse(text: String): PixRecibo? {
        val lower = text.lowercase()

        val valor = extrairValor(text) ?: return null
        val tipo  = detectarTipo(lower)
        val nome  = extrairNome(text, tipo)

        val descricao = when {
            nome.isNotBlank() -> if (tipo == TipoLancamento.DEBITO) "PIX p/ $nome" else "PIX de $nome"
            else              -> if (tipo == TipoLancamento.DEBITO) "Pagamento PIX" else "Recebimento PIX"
        }

        return PixRecibo(valor = valor, tipo = tipo, descricao = descricao)
    }

    private fun extrairValor(text: String): Double? {
        for (pattern in valorPatterns) {
            val match = pattern.find(text) ?: continue
            val raw = match.groupValues[1]
                .replace(".", "")   // remove separador de milhar
                .replace(",", ".")  // vírgula → ponto decimal
            raw.toDoubleOrNull()?.let { if (it > 0) return it }
        }
        return null
    }

    private fun detectarTipo(lower: String): TipoLancamento {
        if (receitaKeywords.any { lower.contains(it) }) return TipoLancamento.RECEITA
        if (debitoKeywords.any  { lower.contains(it) }) return TipoLancamento.DEBITO
        // Se não conseguiu detectar, assume débito (pagamento)
        return TipoLancamento.DEBITO
    }

    private fun extrairNome(text: String, tipo: TipoLancamento): String {
        val patterns = if (tipo == TipoLancamento.DEBITO) receptorPatterns else remetentePatterns
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val nome = match.groupValues[1]
                .trim()
                .split("\n").first()    // só a primeira linha
                .replace(Regex("""[^\p{L}\p{N}\s.\-']"""), "") // remove chars especiais
                .trim()
                .take(60)
            if (nome.length >= 2) return nome
        }
        return ""
    }
}
