package com.gastozen.util

import org.jsoup.Jsoup

/**
 * Parseia o HTML do portal do consumidor NFC-e (SEFAZ estadual).
 *
 * Suporta dois layouts:
 *  - Layout MS/DFe: tabela #tabResult, span.txtTit, span.valor
 *  - Layout federal padrão: classes fixo-prod-serv-* ou tabela #tabela-produto
 */
object NfeHtmlParser {

    fun parse(html: String): NotaFiscal {
        val doc = Jsoup.parse(html)  // Jsoup decodifica &Ocirc; etc. automaticamente

        // ── Emitente ──────────────────────────────────────────────────────────
        val cnpjEmitente = doc.select("[class*=cnpj], #cnpjEmitente, #cnpj")
            .firstOrNull()?.text()?.replace(Regex("[^0-9]"), "") ?: ""

        val dataEmissao = doc.select("[id*=dataemi], [id*=dhemi], #dataEmissao")
            .firstOrNull()?.text() ?: ""

        val produtos = mutableListOf<ProdutoNfe>()

        // ── Estratégia 1: Layout DFe/MS — tabela #tabResult ───────────────────
        // <tr id="Item + N"> com <span class="txtTit"> e <span class="valor">
        val itemRows = doc.select("tr[id^=Item]")
        if (itemRows.isNotEmpty()) {
            for (row in itemRows) {
                val nome = row.selectFirst("span.txtTit")?.text()?.trim()
                    ?: continue
                val valorText = row.selectFirst("span.valor")?.text()?.trim()
                    ?: continue
                val valor = valorText.parseMoeda() ?: continue
                if (valor > 0) produtos.add(ProdutoNfe(nome, "", valor))
            }
        }

        // ── Estratégia 2: Layout federal — classes fixo-prod-serv-* ───────────
        if (produtos.isEmpty()) {
            val descCells = doc.select(".fixo-prod-serv-descricao, [class*=descricao][class*=prod]")
            for (cell in descCells) {
                val nome = cell.text().trim().takeIf { it.isNotBlank() } ?: continue
                val row = cell.parent() ?: continue
                val valorText = row.select(
                    ".fixo-prod-serv-vt, .fixo-prod-serv-vtotal, [class*=vt][class*=prod]"
                ).firstOrNull()?.text()
                    ?: row.select("td").lastOrNull()?.text()
                    ?: continue
                val valor = valorText.parseMoeda() ?: continue
                if (valor > 0) produtos.add(ProdutoNfe(nome, "", valor))
            }
        }

        // ── Estratégia 3: Layout com #tabela-produto ──────────────────────────
        if (produtos.isEmpty()) {
            for (row in doc.select("#tabela-produto tbody tr, #tabela_produto tbody tr")) {
                val cells = row.select("td")
                if (cells.size < 2) continue
                val nome = cells.maxByOrNull { it.text().length }?.text()?.trim()
                    ?.takeIf { it.length > 2 } ?: continue
                val valor = cells.reversed().firstNotNullOfOrNull { it.text().parseMoeda() } ?: continue
                if (valor > 0) produtos.add(ProdutoNfe(nome, "", valor))
            }
        }

        // ── Total ─────────────────────────────────────────────────────────────
        // Layout DFe/MS: .totalNumb.txtMax = "Valor a pagar"
        // Fallback: última ocorrência de .totalNumb ou soma dos produtos
        val valorTotal =
            doc.selectFirst(".totalNumb.txtMax")?.text()?.parseMoeda()
                ?: doc.select(".totalNumb").lastOrNull()?.text()?.parseMoeda()
                ?: doc.select("[id*=vNF], [id*=valorTotal], [class*=total-nf]")
                    .lastOrNull()?.text()?.parseMoeda()
                ?: produtos.sumOf { it.valor }

        if (produtos.isEmpty()) {
            throw Exception(
                "Não foi possível extrair produtos da nota fiscal.\n" +
                "Use o botão Reportar para enviar os detalhes."
            )
        }

        return NotaFiscal(produtos, valorTotal, dataEmissao, cnpjEmitente)
    }

    /** Converte "R$ 1.234,56" ou "1.234,56" para Double. */
    private fun String.parseMoeda(): Double? {
        val limpo = replace(Regex("[R$\\s\u00a0]"), "").trim()
        if (limpo.isBlank()) return null
        return if (limpo.contains(",")) {
            limpo.replace(".", "").replace(",", ".").toDoubleOrNull()
        } else {
            limpo.toDoubleOrNull()
        }
    }
}
