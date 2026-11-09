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
    }
}
