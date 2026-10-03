package com.joakim.rfidmanager.nfc

/** Lock-byten på sida 2 hos Ultralight/NTAG (flyttad från ScanScreen så att den kan enhetstestas). */
object TagLocks {
    fun parseLockedPages(fullSectors: Map<Int, String>): Set<Int> {
        val page2 = fullSectors[2] ?: return emptySet()
        val bytes = page2.split(" ").mapNotNull { it.toIntOrNull(16) }
        if (bytes.size < 2) return emptySet()
        val lb0 = bytes[0]
        val lb1 = bytes[1]
        val locked = mutableSetOf<Int>()
        for (bit in 0..3) {
            if ((lb0 shr bit) and 1 == 1) {
                locked.addAll((4 + bit * 4) until (8 + bit * 4))
            }
        }
        for (bit in 0..3) {
            if ((lb1 shr bit) and 1 == 1) {
                locked.addAll((20 + bit * 4) until (24 + bit * 4))
            }
        }
        return locked
    }

    fun isPageLocked(page: Int, lockedPages: Set<Int>): Boolean {
        if (page < 4) return true
        return page in lockedPages
    }
}
