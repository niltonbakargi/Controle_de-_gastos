package com.gastozen.data.db

import androidx.room.TypeConverter
import com.gastozen.data.model.TipoConta
import com.gastozen.data.model.TipoLancamento
import com.gastozen.data.model.TipoPagamento

class Converters {
    @TypeConverter fun fromTipoConta(value: TipoConta): String = value.name
    @TypeConverter fun toTipoConta(value: String): TipoConta = TipoConta.valueOf(value)

    @TypeConverter fun fromTipoLancamento(value: TipoLancamento): String = value.name
    @TypeConverter fun toTipoLancamento(value: String): TipoLancamento = TipoLancamento.valueOf(value)

    @TypeConverter fun fromTipoPagamento(value: TipoPagamento): String = value.name
    @TypeConverter fun toTipoPagamento(value: String): TipoPagamento = TipoPagamento.valueOf(value)
}
