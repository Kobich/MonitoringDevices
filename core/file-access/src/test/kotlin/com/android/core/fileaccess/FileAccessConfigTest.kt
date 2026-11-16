package com.android.core.fileaccess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileAccessConfigTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `the config from the spec is read`() {
        val problems = mutableListOf<String>()
        val config = FileAccessConfigJson.parse(
            """
            { "parameters": {
                "code_sequence": ["1", "2"],
                "menu": [
                    { "key": "import", "password": "" },
                    { "key": "export", "select": true, "password": "0000" }
                ],
                "initial_files": [
                    { "path": "/file-manager//Contract.pdf", "message": "", "password": "1234" },
                    { "path": "file-manager/Budget.csv", "message": "Ask the administrator", "password": "1234" },
                    { "path": "file-manager/Notes.md" }
                ]
            } }
            """,
            onProblem = problems::add
        )!!

        assertEquals(emptyList<String>(), problems)
        assertEquals(Access.Allowed, config.access(FileAction.IMPORT))
        assertEquals(Access.PasswordRequired(Password("0000")), config.access(FileAction.EXPORT))
        assertEquals(Access.Hidden, config.access(FileAction.DELETE))
        assertEquals(Access.PasswordRequired(Password("1234")), config.access("file-manager/Contract.pdf"))
        assertEquals(Access.Message("Ask the administrator"), config.access("file-manager/Budget.csv"))
        assertEquals(Access.Allowed, config.access("file-manager/Notes.md"))
        assertEquals(Access.Allowed, config.access("file-manager/contract.pdf"))
        assertEquals(Access.Allowed, config.access("Contract.pdf"))
    }

    @Test
    fun `without a menu every action is available, an empty menu has none`() {
        val noMenu = FileAccessConfigJson.parse("""{ "parameters": { "initial_files": [] } }""")!!
        FileAction.entries.forEach { action -> assertEquals(Access.Allowed, noMenu.access(action)) }

        val emptyMenu = FileAccessConfigJson.parse("""{ "parameters": { "menu": [] } }""")!!
        FileAction.entries.forEach { action -> assertEquals(Access.Hidden, emptyMenu.access(action)) }

        assertEquals(Access.Allowed, FileAccessConfig.Unrestricted.access(FileAction.DELETE))
        assertEquals(Access.Allowed, FileAccessConfig.Unrestricted.access("a.pdf"))
    }

    @Test
    fun `a broken config is no config`() {
        listOf("", "not json", "[]", """{ "parameters": { "menu": {} } }""", """{ "menu": "import" }""").forEach { json ->
            assertNull(json, FileAccessConfigJson.parse(json))
        }
    }

    @Test
    fun `bad entries are skipped and reported`() {
        val problems = mutableListOf<String>()
        val config = FileAccessConfigJson.parse(
            """
            { "parameters": {
                "menu": [
                    { "key": "rename" }, 1, { "key": "delete", "select": false },
                    { "key": "export" }, { "key": "export", "password": "x" }, { "key": "import", "password": null }
                ],
                "initial_files": [
                    { "path": "../secret.pdf", "password": "1" }, { "path": "" }, { "message": "no path" },
                    { "path": "a.pdf", "password": "1" }, { "path": "/a.pdf", "password": "2" }
                ]
            } }
            """,
            onProblem = problems::add
        )!!

        assertEquals(Access.Allowed, config.access(FileAction.EXPORT))
        assertEquals(Access.Allowed, config.access(FileAction.IMPORT))
        assertEquals(Access.Hidden, config.access(FileAction.DELETE))
        assertEquals(Access.PasswordRequired(Password("1")), config.access("a.pdf"))
        assertEquals(setOf("a.pdf"), config.files.keys)
        assertEquals(7, problems.size)
    }

    @Test
    fun `paths are normalized and must stay under the root`() {
        assertEquals("a/b/c.pdf", FileAccessConfig.normalizePath("/a//b/./c.pdf/"))
        assertEquals("Docs/q1.xlsx", FileAccessConfig.normalizePath("Docs\\q1.xlsx"))
        listOf("", "/", "./", "a/../b").forEach { path ->
            assertThrows(path, IllegalArgumentException::class.java) { FileAccessConfig.normalizePath(path) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            FileAccessConfig(null, mapOf("a.pdf" to FileRule(null, null), "/a.pdf" to FileRule(null, null)))
        }
    }

    @Test
    fun `a file is found by its path from the root`() {
        val root = File(folder.root, "storage")
        assertEquals("file-manager/Docs/a.pdf", AppFilesRoot.pathOf(File(root, "file-manager/Docs/../Docs/a.pdf"), root))
        assertNull(AppFilesRoot.pathOf(root, root))
        assertNull(AppFilesRoot.pathOf(File(folder.root, "storage2/a.pdf"), root))
        assertNull(AppFilesRoot.pathOf(File(folder.root, "a.pdf"), root))
    }

    @Test
    fun `passwords are compared exactly and never printed`() {
        assertTrue(Password("пароль 1").matches("пароль 1"))
        assertFalse(Password("пароль 1").matches("пароль1"))
        assertFalse(Password("Abc").matches("abc"))
        assertEquals("Password(***)", Password("secret").toString())
        assertThrows(IllegalArgumentException::class.java) { Password("") }
    }
}
