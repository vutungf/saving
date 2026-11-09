package vn.sotietkiem.data

// Sinh tự động từ bộ dữ liệu VietQR (gói npm vietqr-zero, MIT). Logo nằm trong assets/banks/<code>.webp
enum class BankGroup(val label: String) {
    DIGITAL("Ngân hàng số"),
    DOMESTIC("Ngân hàng thương mại trong nước"),
    FOREIGN("Ngân hàng nước ngoài và liên doanh"),
}

data class Bank(
    val code: String,
    val name: String,
    val fullName: String,
    val group: BankGroup,
    val aliases: String,
    val hasLogo: Boolean,
)

object Banks {
    const val OTHER = "OTHER"

    val all: List<Bank> = listOf(
    Bank("CAKE", "Cake", "TMCP Việt Nam Thịnh Vượng - Ngân hàng số CAKE by VPBank", BankGroup.DIGITAL, "cake by vpbank", true),
    Bank("UBANK", "Ubank", "TMCP Việt Nam Thịnh Vượng - Ngân hàng số Ubank by VPBank", BankGroup.DIGITAL, "ubank by vpbank", true),
    Bank("TIMO", "Timo", "Ngân hàng số Timo by Ban Viet Bank (Timo by Ban Viet Bank)", BankGroup.DIGITAL, "timo by ban viet", true),
    Bank("VIKKI", "Vikki", "Ngân hàng TNHH MTV Số Vikki", BankGroup.DIGITAL, "vikki digital bank dong a", true),
    Bank("PVDB", "PVcomBank Pay", "Ngân hàng TMCP Đại Chúng Việt Nam Ngân hàng số", BankGroup.DIGITAL, "pvcombank pay", true),
    Bank("TNEX", "TNEX", "TNEX", BankGroup.DIGITAL, "tnex msb", false),
    Bank("LIOBANK", "Liobank", "Liobank", BankGroup.DIGITAL, "liobank ocb", false),
    Bank("DIGIMI", "Digimi", "Digimi", BankGroup.DIGITAL, "digimi vietbank", false),
    Bank("VCB", "Vietcombank", "Ngân hàng TMCP Ngoại Thương Việt Nam", BankGroup.DOMESTIC, "ngoai thuong", true),
    Bank("ICB", "VietinBank", "Ngân hàng TMCP Công thương Việt Nam", BankGroup.DOMESTIC, "vietin cong thuong ctg", true),
    Bank("BIDV", "BIDV", "Ngân hàng TMCP Đầu tư và Phát triển Việt Nam", BankGroup.DOMESTIC, "dau tu phat trien", true),
    Bank("VBA", "Agribank", "Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam", BankGroup.DOMESTIC, "nong nghiep agri", true),
    Bank("TCB", "Techcombank", "Ngân hàng TMCP Kỹ thương Việt Nam", BankGroup.DOMESTIC, "ky thuong techcom", true),
    Bank("MB", "MB Bank", "Ngân hàng TMCP Quân đội", BankGroup.DOMESTIC, "quan doi mbbank", true),
    Bank("ACB", "ACB", "Ngân hàng TMCP Á Châu", BankGroup.DOMESTIC, "a chau", true),
    Bank("VPB", "VPBank", "Ngân hàng TMCP Việt Nam Thịnh Vượng", BankGroup.DOMESTIC, "thinh vuong", true),
    Bank("TPB", "TPBank", "Ngân hàng TMCP Tiên Phong", BankGroup.DOMESTIC, "tien phong", true),
    Bank("STB", "Sacombank", "Ngân hàng TMCP Sài Gòn Thương Tín", BankGroup.DOMESTIC, "sai gon thuong tin", true),
    Bank("HDB", "HDBank", "Ngân hàng TMCP Phát triển Thành phố Hồ Chí Minh", BankGroup.DOMESTIC, "phat trien tphcm", true),
    Bank("VIB", "VIB", "Ngân hàng TMCP Quốc tế Việt Nam", BankGroup.DOMESTIC, "quoc te", true),
    Bank("SHB", "SHB", "Ngân hàng TMCP Sài Gòn - Hà Nội", BankGroup.DOMESTIC, "sai gon ha noi", true),
    Bank("EIB", "Eximbank", "Ngân hàng TMCP Xuất Nhập khẩu Việt Nam", BankGroup.DOMESTIC, "xuat nhap khau exim", true),
    Bank("MSB", "MSB", "Ngân hàng TMCP Hàng Hải Việt Nam", BankGroup.DOMESTIC, "hang hai maritime", true),
    Bank("OCB", "OCB", "Ngân hàng TMCP Phương Đông", BankGroup.DOMESTIC, "phuong dong", true),
    Bank("SEAB", "SeABank", "Ngân hàng TMCP Đông Nam Á", BankGroup.DOMESTIC, "dong nam a", true),
    Bank("LPB", "LPBank", "Ngân hàng TMCP Lộc Phát Việt Nam", BankGroup.DOMESTIC, "loc phat lienviet", true),
    Bank("VCCB", "BVBank", "Ngân hàng TMCP Bản Việt", BankGroup.DOMESTIC, "ban viet viet capital", true),
    Bank("NAB", "Nam A Bank", "Ngân hàng TMCP Nam Á", BankGroup.DOMESTIC, "nam a", true),
    Bank("BAB", "Bac A Bank", "Ngân hàng TMCP Bắc Á", BankGroup.DOMESTIC, "bac a", true),
    Bank("ABB", "ABBANK", "Ngân hàng TMCP An Bình", BankGroup.DOMESTIC, "an binh", true),
    Bank("PVCB", "PVcomBank", "Ngân hàng TMCP Đại Chúng Việt Nam", BankGroup.DOMESTIC, "dai chung", true),
    Bank("NCB", "NCB", "Ngân hàng TMCP Quốc Dân", BankGroup.DOMESTIC, "quoc dan", true),
    Bank("PGB", "PGBank", "Ngân hàng TMCP Thịnh vượng và Phát triển", BankGroup.DOMESTIC, "thinh vuong phat trien", true),
    Bank("VIETBANK", "VietBank", "Ngân hàng TMCP Việt Nam Thương Tín", BankGroup.DOMESTIC, "viet nam thuong tin", true),
    Bank("VAB", "VietABank", "Ngân hàng TMCP Việt Á", BankGroup.DOMESTIC, "viet a", true),
    Bank("BVB", "BaoViet Bank", "Ngân hàng TMCP Bảo Việt", BankGroup.DOMESTIC, "bao viet", true),
    Bank("KLB", "KienlongBank", "Ngân hàng TMCP Kiên Long", BankGroup.DOMESTIC, "kien long", true),
    Bank("SGICB", "Saigonbank", "Ngân hàng TMCP Sài Gòn Công Thương", BankGroup.DOMESTIC, "sai gon cong thuong", true),
    Bank("SCB", "SCB", "Ngân hàng TMCP Sài Gòn", BankGroup.DOMESTIC, "sai gon", true),
    Bank("MBV", "MBV", "Ngân hàng TNHH MTV Việt Nam Hiện Đại", BankGroup.DOMESTIC, "viet nam hien dai oceanbank", true),
    Bank("CBB", "CB Bank", "Ngân hàng Thương mại TNHH MTV Xây dựng Việt Nam", BankGroup.DOMESTIC, "xay dung", true),
    Bank("GPB", "GPBank", "Ngân hàng Thương mại TNHH MTV Dầu Khí Toàn Cầu", BankGroup.DOMESTIC, "dau khi toan cau", true),
    Bank("COOPBANK", "Co-opBank", "Ngân hàng Hợp tác xã Việt Nam", BankGroup.DOMESTIC, "hop tac xa", true),
    Bank("HSBC", "HSBC", "Ngân hàng TNHH MTV HSBC (Việt Nam)", BankGroup.FOREIGN, "", true),
    Bank("SCVN", "Standard Chartered", "Ngân hàng TNHH MTV Standard Chartered Bank Việt Nam", BankGroup.FOREIGN, "sc", true),
    Bank("SHBVN", "Shinhan Bank", "Ngân hàng TNHH MTV Shinhan Việt Nam", BankGroup.FOREIGN, "shinhan", true),
    Bank("WVN", "Woori Bank", "Ngân hàng TNHH MTV Woori Việt Nam", BankGroup.FOREIGN, "woori", true),
    Bank("UOB", "UOB", "Ngân hàng United Overseas - Chi nhánh TP. Hồ Chí Minh", BankGroup.FOREIGN, "united overseas", true),
    Bank("CIMB", "CIMB", "Ngân hàng TNHH MTV CIMB Việt Nam", BankGroup.FOREIGN, "", true),
    Bank("PBVN", "Public Bank", "Ngân hàng TNHH MTV Public Việt Nam", BankGroup.FOREIGN, "public", true),
    Bank("HLBVN", "Hong Leong", "Ngân hàng TNHH MTV Hong Leong Việt Nam", BankGroup.FOREIGN, "hongleong", true),
    Bank("KEBHANA", "KEB Hana", "Ngân hàng KEB Hana – Chi nhánh Hà Nội", BankGroup.FOREIGN, "hana", true),
    Bank("KOOKMIN", "Kookmin", "Ngân hàng Kookmin - Chi nhánh Hà Nội", BankGroup.FOREIGN, "kb kookmin", true),
    Bank("IBK", "IBK", "Ngân hàng Công nghiệp Hàn Quốc - Chi nhánh Hà Nội", BankGroup.FOREIGN, "cong nghiep han quoc", true),
    Bank("DBS", "DBS Bank", "DBS Bank Ltd - Chi nhánh Thành phố Hồ Chí Minh", BankGroup.FOREIGN, "", true),
    Bank("KBANK", "KBank", "Ngân hàng Đại chúng TNHH Kasikornbank", BankGroup.FOREIGN, "kasikornbank", true),
    Bank("IVB", "Indovina Bank", "Ngân hàng TNHH Indovina", BankGroup.FOREIGN, "indovina", true),
    Bank("VRB", "VRB", "Ngân hàng Liên doanh Việt - Nga", BankGroup.FOREIGN, "viet nga", true),
    )

    private val byCode = all.associateBy { it.code.uppercase() }

    fun find(code: String?): Bank? = code?.let { byCode[it.uppercase()] }
}
