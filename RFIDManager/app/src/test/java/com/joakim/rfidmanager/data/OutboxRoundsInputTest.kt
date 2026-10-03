package com.joakim.rfidmanager.data

import com.joakim.rfidmanager.data.settings.OutboxRoundsInput
import com.joakim.rfidmanager.data.settings.OutboxRoundsInput.Field
import com.joakim.rfidmanager.data.settings.OutboxRoundsInput.Result
import com.joakim.rfidmanager.outbox.core.RoundsConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class OutboxRoundsInputTest {
    private fun invalid(a: String, p: String, r: String) = (OutboxRoundsInput.validate(a, p, r) as Result.Invalid).fields

    @Test fun standardvärden_är_giltiga() {
        val d = RoundsConfig()
        assertEquals(
            Result.Valid(RoundsConfig(12, 60, 3)),
            OutboxRoundsInput.validate(d.attemptsPerRound.toString(), d.pauseMinutes.toString(), d.rounds.toString())
        )
    }

    @Test fun gränsvärden_accepteras() {
        assertEquals(Result.Valid(RoundsConfig(1, 1, 1)), OutboxRoundsInput.validate("1", "1", "1"))
        assertEquals(Result.Valid(RoundsConfig(100, 1440, 20)), OutboxRoundsInput.validate("100", "1440", "20"))
    }

    @Test fun utanför_gränserna_nekas_per_fält() {
        assertEquals(setOf(Field.ATTEMPTS), invalid("0", "60", "3"))
        assertEquals(setOf(Field.ATTEMPTS), invalid("101", "60", "3"))
        assertEquals(setOf(Field.PAUSE_MINUTES), invalid("12", "0", "3"))
        assertEquals(setOf(Field.PAUSE_MINUTES), invalid("12", "1441", "3"))
        assertEquals(setOf(Field.ROUNDS), invalid("12", "60", "0"))
        assertEquals(setOf(Field.ROUNDS), invalid("12", "60", "21"))
    }

    @Test fun tomt_negativt_decimal_och_text_nekas() {
        assertEquals(setOf(Field.ATTEMPTS, Field.PAUSE_MINUTES, Field.ROUNDS), invalid("", "", ""))
        assertEquals(setOf(Field.ATTEMPTS), invalid("-5", "60", "3"))
        assertEquals(setOf(Field.PAUSE_MINUTES), invalid("12", "1.5", "3"))
        assertEquals(setOf(Field.ROUNDS), invalid("12", "60", "tre"))
        assertEquals(setOf(Field.ATTEMPTS), invalid("99999999999", "60", "3")) // större än Int
    }

    @Test fun blanksteg_runt_siffrorna_tillåts() {
        assertEquals(Result.Valid(RoundsConfig(5, 30, 2)), OutboxRoundsInput.validate(" 5 ", "30 ", " 2"))
    }

    @Test fun flera_fel_rapporteras_samtidigt() {
        assertEquals(setOf(Field.ATTEMPTS, Field.ROUNDS), invalid("0", "60", "99"))
    }
}
