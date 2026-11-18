plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.11"

stonecutter parameters {
    val loaderName = branch.id.ifEmpty { "fabric" }
    constants.match(loaderName, "fabric", "forge")
    replacements {
        string(current.parsed < "1.21.11") {
            replace("net.minecraft.resources.Identifier", "net.minecraft.resources.ResourceLocation")
            replace("Identifier", "ResourceLocation")
            replace("AvatarRenderer", "PlayerRenderer")
        }
        string(current.parsed >= "26.1") {
            replace("net.fabricmc.fabric.api.client.keybinding.v1", "net.fabricmc.fabric.api.client.keymapping.v1")
            replace("KeyBindingHelper", "KeyMappingHelper")
            replace("registerKeyBinding", "registerKeyMapping")
            replace("renderWidget", "extractWidgetRenderState")
            replace("renderContents", "extractContents")
            replace("renderBackground(", "extractBackground(")
            replace(".drawString(", ".text(")
            replace(".renderItem(", ".item(")
            replace("renderEntityInInventoryFollowsMouse", "extractEntityInInventoryFollowsMouse")
        }
        regex(current.parsed >= "26.1") {
            replace(
                "(?<![A-Za-z0-9_])GuiGraphics\\b",
                "GuiGraphicsExtractor",
                "(?<![A-Za-z0-9_])GuiGraphicsExtractor\\b",
                "GuiGraphics",
            )
        }
        string(current.parsed >= "26.3") {
            replace("mc.screen", "mc.gui.screen()")
            replace("mc.setScreen(", "mc.gui.setScreen(")
            replace("minecraft.setScreen(", "minecraft.gui.setScreen(")
            replace("mc.getOverlay()", "mc.gui.overlay()")
            replace("mc.getMainRenderTarget()", "mc.gameRenderer.mainRenderTarget()")
            replace("mc.resizeDisplay()", "mc.resizeGui()")
            replace("import net.minecraft.client.resources.language.I18n;", "import net.minecraft.locale.Language;")
            replace("I18n.exists(", "Language.getInstance().has(")
            replace("pose.mulPose(Axis.", "pose.rotate(Axis.")
            replace("mc.player.swinging", "mc.player.isSwinging()")
            replace("InputConstants.Type.KEYSYM", "InputConstants.Type.KEYBOARD")
            replace("GLFW.GLFW_KEY_UNKNOWN", "InputConstants.UNKNOWN.getValue()")
            replace("GLFW.GLFW_KEY_ENTER", "InputConstants.KEY_RETURN")
            replace("GLFW.GLFW_KEY_PAGE_UP", "InputConstants.KEY_PAGEUP")
            replace("GLFW.GLFW_KEY_PAGE_DOWN", "InputConstants.KEY_PAGEDOWN")
            for (input in listOf("KEY_1", "KEY_8", "KEY_B", "KEY_F", "KEY_SPACE", "KEY_LEFT", "KEY_RIGHT",
                    "KEY_UP", "KEY_DOWN", "MOUSE_BUTTON_LEFT", "MOUSE_BUTTON_RIGHT")) {
                replace("GLFW.GLFW_$input", "InputConstants.$input")
            }
        }
    }
}
