package com.arroom.characters.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GltfValidatorTest {

    private fun glbHeader(payload: String = ""): ByteArray =
        "glTF".toByteArray(Charsets.ISO_8859_1) + payload.toByteArray(Charsets.ISO_8859_1)

    @Test
    fun `accepts binary glb`() {
        val verdict = GltfValidator.check(1_000_000, glbHeader("""{"asset":{"version":"2.0"}}"""))
        assertEquals(GltfValidator.Verdict.Ok, verdict)
    }

    @Test
    fun `accepts text gltf json`() {
        val head = """   {"asset": {"version": "2.0"}}""".toByteArray()
        assertEquals(GltfValidator.Verdict.Ok, GltfValidator.check(2048, head))
    }

    @Test
    fun `rejects file over size limit`() {
        val verdict = GltfValidator.check(GltfValidator.MAX_SIZE_BYTES + 1, glbHeader())
        assertTrue(verdict is GltfValidator.Verdict.TooBig)
        assertEquals(GltfValidator.MAX_SIZE_MB, (verdict as GltfValidator.Verdict.TooBig).limitMb)
    }

    @Test
    fun `rejects foreign format`() {
        // Сигнатура ZIP — типичный случай, когда человек выбирает архив
        val zip = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00)
        assertEquals(GltfValidator.Verdict.NotGltf, GltfValidator.check(4096, zip))
    }

    @Test
    fun `rejects draco compressed model`() {
        val head = glbHeader("""{"extensionsUsed":["KHR_draco_mesh_compression"]}""")
        val verdict = GltfValidator.check(500_000, head)
        assertTrue(verdict is GltfValidator.Verdict.Compressed)
        assertEquals("Draco", (verdict as GltfValidator.Verdict.Compressed).label)
    }

    @Test
    fun `rejects meshopt compressed model`() {
        val head = glbHeader("""{"extensionsUsed":["EXT_meshopt_compression"]}""")
        val verdict = GltfValidator.check(500_000, head)
        assertEquals("Meshopt", (verdict as GltfValidator.Verdict.Compressed).label)
    }

    @Test
    fun `rejects truncated file`() {
        assertEquals(GltfValidator.Verdict.Unreadable, GltfValidator.check(3, byteArrayOf(1, 2, 3)))
    }

    @Test
    fun `binary payload after json chunk does not crash the scan`() {
        val head = glbHeader("""{"asset":{"version":"2.0"}}""") +
            ByteArray(2048) { (it % 256).toByte() }
        assertEquals(GltfValidator.Verdict.Ok, GltfValidator.check(50_000, head))
    }
}
