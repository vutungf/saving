package vn.sotietkiem

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.sotietkiem.data.Account
import vn.sotietkiem.data.Banks
import vn.sotietkiem.data.Codec
import vn.sotietkiem.data.InterestPayout
import vn.sotietkiem.domain.Calc
import vn.sotietkiem.domain.Status
import vn.sotietkiem.domain.bankAllocation
import vn.sotietkiem.domain.cashflow12
import vn.sotietkiem.util.Fmt
import vn.sotietkiem.util.VnWords
import java.time.LocalDate

class CalcTest {
    private val today = LocalDate.of(2026, 11, 9)
    private fun acc(start: String, term: Int, closed: Boolean = false) = Account(
        "x", "n", 100_000_000, start, term, "", 5.0, InterestPayout.MATURITY, "VCB", "Vietcombank", "", closed, null, "", "",
    )

    @Test fun maturityClampsToMonthEnd() {
        assertEquals(LocalDate.of(2026, 2, 28), Calc.maturityDate(LocalDate.of(2026, 1, 31), 1))
        assertEquals(LocalDate.of(2028, 2, 29), Calc.maturityDate(LocalDate.of(2028, 1, 31), 1))
    }

    @Test fun simpleInterest() {
        assertEquals(365L, Calc.days(LocalDate.of(2026, 4, 9), LocalDate.of(2027, 4, 9)))
        assertEquals(24_000_000L, Calc.interest(500_000_000, 4.8, 365))
    }

    @Test fun status() {
        assertEquals(Status.ACTIVE, Calc.status(acc("2026-11-01", 6), today))
        assertEquals(Status.SOON, Calc.status(acc("2026-10-20", 1), today))
        assertEquals(Status.MATURED, Calc.status(acc("2026-10-09", 1), today))
        assertEquals(Status.CLOSED, Calc.status(acc("2026-01-01", 1, true), today))
    }

    @Test fun formatting() {
        assertEquals("150.000.000", Fmt.group(150_000_000))
        assertEquals(5.2, Fmt.parseRate("5,2")!!, 1e-9)
        assertEquals("ngan hang dong a", Fmt.fold("Ngân hàng Đông Á"))
        assertEquals("Một trăm năm mươi triệu đồng", VnWords.read(150_000_000))
        assertEquals("Một nghìn không trăm lẻ năm đồng", VnWords.read(1005))
        assertEquals("Hai mươi tư đồng", VnWords.read(24))
    }

    @Test fun charts() {
        val cf = cashflow12(listOf(acc("2026-01-01", 1), acc("2026-12-15", 1)), today)
        assertEquals(100_000_000L, cf[0].principal)
        assertEquals(100_000_000L, cf[2].principal)
        val many = (1..9).map { acc("2026-11-01", 6).copy(bankCode = "B$it", bankName = "B$it", amount = it * 1000L) }
        assertEquals(7, bankAllocation(many).size)
        assertEquals(58, Banks.all.size)
    }

    @Test fun importAcceptsThreeShapesAndSkipsInvalid() {
        val one = """{"name":"A","amount":1000000,"startDate":"2026-01-31","termMonths":1,"interestRate":"5,2","bankCode":"vcb"}"""
        val bad = """{"name":"","amount":0}"""
        for (text in listOf("""{"accounts":[$one,$bad]}""", """{"books":[$one,$bad]}""", "[$one,$bad]")) {
            val r = Codec.parseImport(text)
            assertEquals(1, r.accounts.size)
            assertEquals(1, r.skipped)
            assertEquals("2026-02-28", r.accounts[0].maturityDate)
            assertEquals("VCB", r.accounts[0].bankCode)
            assertEquals(5.2, r.accounts[0].interestRate, 1e-9)
        }
        val round = Codec.parseImport(Codec.export(Codec.parseImport("[$one]").accounts))
        assertEquals(1, round.accounts.size)
    }
}
