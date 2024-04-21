package Infraestructure.DataAccess.Entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Monitoring")
data class Monitoring(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val data: String,
        val label: String,
        val velocidadeRodaEsq: Double,
        val velocidadeRodaDir: Double,
        val posicaoXAlmejada: Double,
        val posicaoYAlmejada: Double,
        val pontoEvento: Int
)