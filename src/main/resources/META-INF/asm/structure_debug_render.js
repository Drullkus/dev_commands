// noinspection JSUnusedGlobalSymbols

var ASM = Java.type('net.neoforged.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');

var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');

function initializeCoreMod() {
    return {
        'render': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.renderer.debug.DebugRenderer',
                'methodName': 'render',
                'methodDesc': '(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V'
            },
            'transformer': function (/*org.objectweb.asm.tree.MethodNode*/ methodNode) {
                var /*org.objectweb.asm.tree.InsnList*/ instructions = methodNode.instructions;

                instructions.insert(ASM.listOf(
                    new VarInsnNode(Opcodes.ALOAD, 0),
                    new FieldInsnNode(
                        Opcodes.GETFIELD,
                        'net/minecraft/client/renderer/debug/DebugRenderer',
                        'structureRenderer',
                        'Lnet/minecraft/client/renderer/debug/StructureRenderer;'
                    ),
                    new VarInsnNode(Opcodes.ALOAD, 1),
                    new VarInsnNode(Opcodes.ALOAD, 2),
                    new VarInsnNode(Opcodes.DLOAD, 3),
                    new VarInsnNode(Opcodes.DLOAD, 5),
                    new VarInsnNode(Opcodes.DLOAD, 7),
                    new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        'net/minecraft/client/renderer/debug/StructureRenderer',
                        'render',
                        '(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;DDD)V'
                    )
                ));

                return methodNode;
            }
        }
    }
}
