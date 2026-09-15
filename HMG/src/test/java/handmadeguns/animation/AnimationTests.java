package handmadeguns.animation;

import handmadeguns.client.modelLoader.blockbench.BlockbenchProject;
import handmadeguns.client.modelLoader.blockbench.BedrockAnimationLoader;
import handmadeguns.client.modelLoader.blockbench.BedrockGeometryLoader;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/** Dependency-free regression entry point: Gradle animationTest, or Java 8 + Minecraft's Gson. */
public final class AnimationTests {
    private static int checks;
    private static final AnimationLoader LOADER = new AnimationLoader();

    public static void main(String[] args) throws Exception {
        parsing(); bedrockParsing(); bedrockGeometry(); evaluation(); transitions(); locomotion(); events(); isolationAndClock(); reloadBridge(); legacyReloadRegression();
        if (args.length > 0) {
            AnimationDefinition example = LOADER.load(new File(args[0]));
            example.validateParts(new LinkedHashSet<String>(Arrays.asList("bolt", "magazine")));
            for (String clip : Arrays.asList("idle", "fire", "reload", "inspect")) check(example.clips.containsKey(clip), "example " + clip);
            check(LOADER.load(new File(args[0])) == example, "definition cache identity");
            LOADER.invalidate(new File(args[0]));
            check(LOADER.load(new File(args[0])) != example, "cache invalidation");
        }
        if (args.length > 1) blockbenchReload(new File(args[1]));
        System.out.println("HMG animation: " + checks + " checks passed (HMG/Bedrock parsers, evaluator, layers, locomotion, events, isolation, reload bridge).");
    }

    private static void blockbenchReload(File file) throws Exception {
        BlockbenchProject project = new BlockbenchProject(file, file.getParentFile());
        AnimationClip reload = project.animations.requireClip("reload_tactical");
        check(project.animations.clips.containsKey("reload"), "TaCZ tactical reload alias");
        check(project.animations.clips.containsKey("reload_empty"), "TaCZ dry reload alias");
        near(reload.duration,2.6,"TaCZ tactical reload duration");
        String root = null;
        for (BlockbenchProject.Node node : project.nodes.values()) if ("root".equals(node.name)) root=node.uuid;
        check(root != null && reload.tracks.containsKey(root), "TaCZ reload animates gun root");
        AnimationController controller = new AnimationController(project.animations);
        controller.advanceTo(0,null);
        check(controller.play(AnimationController.Layer.ACTION,"reload_tactical",true,1,null), "TaCZ reload request accepted");
        controller.advanceTo(.5,null);
        AnimationPose.Transform pose = controller.sample(AnimationPose.EMPTY).get(root);
        check(Math.abs(pose.x)+Math.abs(pose.y)+Math.abs(pose.z)+Math.abs(pose.rx)+Math.abs(pose.ry)+Math.abs(pose.rz)>0,
                "TaCZ reload contributes rendered root pose");
        near(controller.progress(AnimationController.Layer.ACTION),.5/2.6,"TaCZ reload clock advances");
        File packs = new File(file.getParentFile().getParentFile(), "src/main/resources/hmg_packs");
        for (String name : Arrays.asList("RPK", "AKS74U")) {
            File gun = new File(packs,"GVCguns/guns/" + name + ".txt");
            boolean movingReload = false, importedDirective = false;
            for (String line : java.nio.file.Files.readAllLines(gun.toPath(), java.nio.charset.Charset.forName("Shift_JIS"))) {
                line = line.trim();
                importedDirective |= line.startsWith("Animations,") || line.startsWith("BedrockModel,");
                if (!line.startsWith("AddReloadMotionKey,")) continue;
                String[] fields = line.split(",");
                if (fields.length != 15) continue;
                float[] values = new float[14];
                for (int i=0;i<14;i++) values[i] = Float.parseFloat(fields[i+1].trim());
                handmadeguns.client.render.HMGGunParts_Motion motion = new handmadeguns.client.render.HMGGunParts_Motion();
                motion.set(values[0],values[1],values[2],values[3],values[4],values[5],values[6],
                        values[7],values[8],values[9],values[10],values[11],values[12],values[13]);
                handmadeguns.client.render.HMGGunParts_Motion_PosAndRotation sample = motion.posAndRotation((values[0]+values[7])/2);
                near(sample.rotationX,(values[4]+values[11])/2,name + " authored reload X interpolation");
                near(sample.rotationZ,(values[6]+values[13])/2,name + " authored reload Z interpolation");
                movingReload |= sample.rotationX != 0 || sample.rotationZ != 0 || sample.posY != 0;
            }
            check(!importedDirective, name + " stays on legacy animation path");
            // RPK uses the legacy compatible-parts generator; AKS has explicit authored keys.
            if ("AKS74U".equals(name)) check(movingReload,"AKS authored reload produces a non-neutral pose without Minecraft");
        }
        File compatibility = new File(packs, "TaCZCompatibility");
        BlockbenchProject ak = BedrockGeometryLoader.load(
                new File(compatibility, "models/ak47_geo.json"),
                new File(compatibility, "textures/models/ak47.png"));
        BlockbenchProject glock = BedrockGeometryLoader.load(
                new File(compatibility, "models/glock_17_geo.json"),
                new File(compatibility, "textures/models/glock_17.png"));
        check(!ak.nodes.get("mount").visible && ak.nodes.get("rail2").parent == ak.nodes.get("mount"),
                "attachment-free AK hides the mount and its sight-rail subtree");
        check(!ak.nodes.get("additional_magazine").visible && !glock.nodes.get("additional_magazine").visible,
                "animation-only additional magazines are hidden in the base state");
        check(glock.nodes.get("mag_standard").visible
                        && !glock.nodes.get("mag_extended_1").visible
                        && !glock.nodes.get("mag_extended_2").visible
                        && !glock.nodes.get("mag_extended_3").visible,
                "attachment-free Glock renders only its standard magazine");
        check(ak.nodes.get("fixed").visible && ak.nodes.get("thirdperson_hand").visible,
                "Bedrock item-positioning nodes remain available");
    }

    private static AnimationDefinition parse(String clips) throws IOException {
        return LOADER.parse(new StringReader("{\"formatVersion\":1,\"clips\":{" + clips + "}}"), "test.json");
    }

    private static void parsing() throws Exception {
        AnimationDefinition valid = parse("\"custom\":{\"duration\":2,\"futureField\":{},\"parts\":{\"bolt\":[{\"time\":0},{\"time\":2,\"position\":[1,2,3]}]}}");
        check(valid.clips.containsKey("custom"), "arbitrary clip");
        valid.validateParts(Collections.singleton("bolt"));
        expectFailure(() -> valid.requireClip("missing"), "unknown clip");
        expectFailure(() -> valid.validateParts(Collections.singleton("body")), "Clip: custom | Part: bolt");
        expectFailure(() -> LOADER.parse(new StringReader("{broken}"), "bad.json"), "bad.json");
        expectFailure(() -> LOADER.parse(new StringReader("{\"formatVersion\":1,\"clips\":{}} garbage"), "bad.json"), "bad.json");
        expectFailure(() -> LOADER.parse(new StringReader("{\"formatVersion\":2,\"clips\":{}}"), "bad.json"), "version 1");
        expectFailure(() -> parse("\"x\":{\"duration\":1,\"duration\":2}"), "Duplicate JSON field");
        expectFailure(() -> parse("\"x\":{}"), "Missing 'duration'");
        expectFailure(() -> parse("\"x\":{\"duration\":-1}"), "non-negative");
        expectFailure(() -> parse("\"x\":{\"duration\":1e999}"), "finite number");
        expectFailure(() -> parse("\"x\":{\"duration\":\"NaN\"}"), "finite number");
        expectFailure(() -> parse("\"x\":{\"duration\":1,\"parts\":{\"bolt\":[{\"time\":0,\"rotation\":[0,1e99,0]}]}}"), "Part: bolt | Keyframe: 0 | rotation[1]");
        expectFailure(() -> parse("\"x\":{\"duration\":1,\"parts\":{\"bolt\":[{\"time\":0,\"position\":[0,0]}]}}"), "exactly three");
        expectFailure(() -> parse("\"x\":{\"duration\":1,\"parts\":{\"bolt\":[{\"time\":0},{\"time\":0}]}}"), "strictly increasing");
        expectFailure(() -> parse("\"x\":{\"duration\":1,\"parts\":{\"bolt\":[{\"time\":2}]}}"), "within duration");
        check(parse("\"zero\":{\"duration\":0}").requireClip("zero").duration == 0, "zero once valid");
        check(parse("\"zero\":{\"duration\":0,\"loop\":\"hold\"}").requireClip("zero").loop == AnimationClip.Loop.HOLD, "zero hold valid");
        expectFailure(() -> parse("\"zero\":{\"duration\":0,\"loop\":true}"), "cannot loop");
        check(parse("\"loop\":{\"duration\":1,\"loop\":true}").requireClip("loop").loop == AnimationClip.Loop.LOOP, "loop valid");
        AnimationDefinition simultaneous = parse("\"x\":{\"duration\":1,\"events\":[{\"time\":0.5,\"event\":\"a\"},{\"time\":0.5,\"event\":\"b\"}]}");
        check(simultaneous.requireClip("x").events.get(1).name.equals("b"), "duplicate event times stable");
        expectFailure(() -> valid.clips.clear(), null);
        expectFailure(() -> valid.requireClip("custom").tracks.clear(), null);
        expectFailure(() -> new AnimationPose.Transform(Float.NaN, 0, 0, 0, 0, 0), "Non-finite");
    }

    private static void bedrockParsing() throws Exception {
        String json = "{\"format_version\":\"1.8.0\",\"animations\":{" +
                "\"static_idle\":{\"loop\":true,\"bones\":{\"root\":{\"position\":[1,2,3]}}}," +
                "\"idle\":{\"loop\":true,\"animation_length\":1,\"bones\":{\"constraint\":{\"rotation\":[1,2,3]}}}," +
                "\"walk_forward\":{\"loop\":true,\"bones\":{\"root\":{\"position\":{\"0.0\":[0,0,0],\"0.5\":{\"pre\":[2,0,0],\"post\":[3,0,0],\"lerp_mode\":\"catmullrom\"}}}},\"sound_effects\":{\"0.25\":{\"effect\":\"step\"}}}," +
                "\"shoot\":{\"animation_length\":0.2,\"bones\":{\"bolt\":{\"scale\":0.5}}}}}";
        AnimationDefinition parsed = new BedrockAnimationLoader().parse(new StringReader(json), "bedrock-test");
        check(parsed.clips.containsKey("movement_idle"), "authored Bedrock movement idle alias");
        check(parsed.clips.containsKey("fire"), "Bedrock shoot alias");
        check(parsed.requireClip("static_idle").loop == AnimationClip.Loop.HOLD,
                "zero-duration Bedrock loop holds");
        near(parsed.requireClip("walk_forward").duration,.5,"Bedrock missing duration inferred from keys");
        near(parsed.requireClip("walk_forward").events.get(0).time,.25,"Bedrock presentation event retained");
        AnimationPose.Transform root = parsed.requireClip("walk_forward").tracks.get("root").sample(.5);
        near(root.x,-2 * 3.0/16.0,"Bedrock split-key arrival uses Blockbench conversion");
        near(parsed.requireClip("shoot").tracks.get("bolt").sample(0).sx,.5,"Bedrock scalar scale");
        Set<String> ignored = new LinkedHashSet<String>();
        AnimationDefinition filtered = BedrockAnimationLoader.retainKnownParts(parsed,
                new LinkedHashSet<String>(Arrays.asList("root", "bolt")), ignored);
        check(ignored.contains("constraint") && !filtered.partNames.contains("constraint"),
                "optional Bedrock tracks absent from selected geometry are ignored");
        expectFailure(() -> new BedrockAnimationLoader().parse(new StringReader(
                "{\"animations\":{\"x\":{\"bones\":{\"root\":{\"position\":[\"query.x\",0,0]}}}}}"), "bad"),
                "Unsupported Bedrock/Molang");
    }

    private static void bedrockGeometry() throws Exception {
        String json = "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{"
                + "\"description\":{\"identifier\":\"geometry.test\",\"texture_width\":64,\"texture_height\":32},"
                + "\"bones\":["
                + "{\"name\":\"root\",\"pivot\":[1,2,3],\"rotation\":[10,20,30],\"mirror\":true,\"cubes\":["
                + "{\"origin\":[0,0,0],\"size\":[2,4,6],\"inflate\":0.5,\"uv\":[4,8]},"
                + "{\"origin\":[2,2,2],\"size\":[-1,1,1],\"mirror\":false,\"uv\":[0,0]}]},"
                + "{\"name\":\"child\",\"parent\":\"root\",\"pivot\":[2,4,6],\"cubes\":["
                + "{\"origin\":[1,2,3],\"size\":[2,2,2],\"pivot\":[2,3,4],\"rotation\":[0,45,0],\"uv\":{"
                + "\"north\":{\"uv\":[1,2],\"uv_size\":[3,4]},\"down\":{\"uv\":[8,9],\"uv_size\":[-2,-3]}}}]},"
                + "{\"name\":\"idle_view\",\"parent\":\"root\",\"pivot\":[0,12,4]},"
                + "{\"name\":\"mount\",\"parent\":\"root\"},"
                + "{\"name\":\"attachment_adapter\",\"parent\":\"root\"},"
                + "{\"name\":\"adapter_child\",\"parent\":\"attachment_adapter\"}]}]}";
        BlockbenchProject project = BedrockGeometryLoader.parse(new StringReader(json),
                new BufferedImage(64,32,BufferedImage.TYPE_INT_ARGB), new File("synthetic.geo.json"), "test.png");
        check(project.nodes.size() == 6 && project.roots.size() == 1, "Bedrock hierarchy imported");
        BlockbenchProject.Node root = project.nodes.get("root"), child = project.nodes.get("child");
        check(child.parent == root && root.children.contains(child), "Bedrock parent link");
        check(!project.nodes.get("mount").visible && !project.nodes.get("adapter_child").visible,
                "Bedrock base state hides conditional mount and adapter geometry");
        near(root.origin[0],-1,"Bedrock pivot converted to project space");
        near(root.origin[1],-22,"Bedrock root uses TaCZ 24-pixel Y origin");
        near(root.rotation[2],30,"Bedrock rest rotation");
        check(root.faces.size() == 12 && child.faces.size() == 2, "Bedrock box/per-face/negative-size cubes");
        near(project.textures.get(0).width,64,"Bedrock texture width");
        near(project.textures.get(0).height,32,"Bedrock texture height");
        near(root.faces.get(0).uv[0][0],18.0/64.0,"Bedrock inherited mirror box U");
        near(root.faces.get(0).uv[0][1],18.0/32.0,"Bedrock mirrored box V follows its physical vertex");
        near(root.faces.get(0).uv[1][0],12.0/64.0,"Bedrock mirrored box opposite U");
        near(root.faces.get(0).uv[1][1],18.0/32.0,"Bedrock mirrored box opposite V");
        near(root.faces.get(0).vertices[0][0],-0.28125,"Bedrock mirrored inflate/position X");
        near(root.faces.get(0).vertices[0][1],-0.46875,"Bedrock inflate/position Y");
        near(root.faces.get(0).vertices[0][2],0.65625,"Bedrock inflate/position Z");
        BlockbenchProject.Face negativeBox = root.faces.get(6);
        near(negativeBox.uv[0][0],1.0/64.0,"Bedrock non-mirrored negative-size box U");
        near(negativeBox.uv[0][1],1.0/32.0,"Bedrock non-mirrored negative-size box V");
        near(negativeBox.vertices[0][0],0,"Bedrock negative dimension retains TaCZ endpoint order");
        BlockbenchProject.Face down = child.faces.get(0), north = child.faces.get(1);
        near(north.uv[0][0],4.0/64.0,"Bedrock north per-face U follows TaCZ vertex order");
        near(north.uv[0][1],2.0/32.0,"Bedrock north per-face V follows TaCZ vertex order");
        near(north.uv[3][0],4.0/64.0,"Bedrock north final U corner");
        near(north.uv[3][1],6.0/32.0,"Bedrock north final V corner");
        near(down.uv[0][0],6.0/64.0,"Bedrock negative UV extent U");
        near(down.uv[0][1],9.0/32.0,"Bedrock negative UV extent V");
        near(down.uv[3][0],6.0/64.0,"Bedrock negative UV final U corner");
        near(down.uv[3][1],6.0/32.0,"Bedrock negative UV final V corner");
        expectFailure(() -> BedrockGeometryLoader.parse(new StringReader(json.replace(
                "\"pivot\":[1,2,3]", "\"pivot\":[1,2,3],\"poly_mesh\":{}")),
                new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB), new File("bad.geo.json"), "bad.png"),
                "Unsupported Bedrock poly_mesh");
    }

    private static void evaluation() {
        AnimationTrack track = new AnimationTrack(Arrays.asList(key(1, 0), key(3, 10)));
        near(track.sample(-1).x, 0, "before first");
        near(track.sample(1).x, 0, "exact first");
        near(track.sample(2).x, 5, "interpolation");
        near(track.sample(3).x, 10, "exact last");
        near(track.sample(8).x, 10, "after last");
        AnimationTrack rotation = new AnimationTrack(Arrays.asList(
                new AnimationKeyframe(0, new AnimationPose.Transform(0,0,0,0,0,350)),
                new AnimationKeyframe(1, new AnimationPose.Transform(0,0,0,0,0,10))));
        near(rotation.sample(.5).rz, 180, "Euler linear, no angle wrap");
    }

    private static AnimationClip clip(String name, float x, double fade, int priority, boolean interruptible) {
        return new AnimationClip(name, 10, AnimationClip.Loop.HOLD,
                Collections.singletonMap("bolt", new AnimationTrack(Collections.singletonList(key(0, x)))),
                Collections.<AnimationEvent>emptyList(), fade, fade, priority, interruptible);
    }

    private static AnimationDefinition definition(AnimationClip... clips) {
        Map<String, AnimationClip> result = new LinkedHashMap<String, AnimationClip>();
        for (AnimationClip clip : clips) result.put(clip.name, clip);
        return new AnimationDefinition("test", result);
    }

    private static AnimationPose baseline(float x) {
        return new AnimationPose(Collections.singletonMap("bolt", new AnimationPose.Transform(x,0,0,0,0,0)));
    }

    private static void transitions() {
        AnimationController controller = new AnimationController(definition(clip("inspect",10,1,1,true),
                clip("reload",20,1,2,true), clip("locked",30,0,3,false), clip("fire",2,0,5,true)));
        controller.sample(baseline(0)); controller.advanceTo(0, null);
        check(controller.play(AnimationController.Layer.ACTION,"inspect"), "play");
        near(controller.sample(baseline(0)).get("bolt").x, 0, "fade beginning");
        controller.advanceTo(.5, null);
        near(controller.sample(baseline(0)).get("bolt").x, 5, "fade halfway");
        check(controller.play(AnimationController.Layer.ACTION,"reload"), "interrupt higher priority");
        near(controller.sample(baseline(100)).get("bolt").x, 5, "interrupt captures actual pose despite baseline change");
        controller.advanceTo(1, null);
        near(controller.sample(baseline(0)).get("bolt").x, 12.5, "interrupted fade halfway");
        check(!controller.play(AnimationController.Layer.ACTION,"inspect"), "lower priority rejected");
        controller.advanceTo(1.5, null);
        near(controller.sample(baseline(0)).get("bolt").x, 20, "fade completion");
        check(!controller.transitioning(), "transition complete");
        controller.stop(AnimationController.Layer.ACTION);
        controller.advanceTo(2, null);
        near(controller.sample(baseline(0)).get("bolt").x, 10, "exit fade halfway");
        controller.advanceTo(2.5, null);
        near(controller.sample(baseline(0)).get("bolt").x, 0, "exit fade completion");
        controller.play(AnimationController.Layer.ADDITIVE,"fire");
        near(controller.sample(baseline(7)).get("bolt").x, 9, "additive overlays base");
        controller.play(AnimationController.Layer.ACTION,"locked");
        check(!controller.play(AnimationController.Layer.ACTION,"reload"), "noninterruptible rejected");
        controller.stop(AnimationController.Layer.ACTION);
        check(!controller.active("locked"), "owner cancellation bypasses lock");

        AnimationController states = new AnimationController(definition(clip("idle",0,1,0,true),
                clip("ads",10,1,0,true), clip("instant",20,0,0,true)));
        states.sample(baseline(0)); states.advanceTo(0,null);
        states.play(AnimationController.Layer.BASE,"instant");
        // No intervening sample: the source must include the immediate layer change.
        states.play(AnimationController.Layer.BASE,"ads");
        near(states.sample(baseline(0)).get("bolt").x,20,"back-to-back requests capture evaluated pose");
        states.advanceTo(.5,null);
        near(states.sample(baseline(0)).get("bolt").x,15,"state blend halfway");
        states.play(AnimationController.Layer.BASE,"ads");
        states.advanceTo(1,null);
        near(states.sample(baseline(0)).get("bolt").x,10,"duplicate state does not restart blend");
        check(!states.transitioning(),"duplicate state blend completes");
        states.play(AnimationController.Layer.BASE,"idle"); states.advanceTo(1.25,null);
        near(states.sample(baseline(0)).get("bolt").x,7.5,"ADS exit advances");
        states.play(AnimationController.Layer.BASE,"ads");
        near(states.sample(baseline(0)).get("bolt").x,7.5,"rapid reversal captures visible pose");
        states.advanceTo(2.25,null);
        near(states.sample(baseline(0)).get("bolt").x,10,"reversal reaches destination");
        check(!states.transitioning(),"reversal releases source");

        AnimationController layered = new AnimationController(definition(
                clip("base",10,0,0,true), clip("move",2,0,0,true),
                clip("action",20,0,0,true), clip("kick",3,0,0,true)));
        layered.advanceTo(0,null);
        layered.play(AnimationController.Layer.BASE,"base");
        layered.play(AnimationController.Layer.MOVEMENT,"move");
        near(layered.sample(baseline(0)).get("bolt").x,12,"movement adds to static base");
        layered.play(AnimationController.Layer.ACTION,"action");
        layered.play(AnimationController.Layer.ADDITIVE,"kick");
        near(layered.sample(baseline(0)).get("bolt").x,23,"action overrides movement before additive fire");
    }

    private static void locomotion() {
        Set<String> clips = new LinkedHashSet<String>(Arrays.asList("movement_idle", "walk_forward",
                "walk_backward", "walk_sideway", "walk_aiming", "run_start", "run", "run_hold", "run_end"));
        LocomotionAnimationBridge.State state = new LocomotionAnimationBridge.State();
        LocomotionAnimationBridge.Request request = state.update(locomotion(true,false,false,true,false,
                LocomotionAnimationBridge.Direction.FORWARD), clips, null);
        check(request != null && "movement_idle".equals(request.clip) && request.loop == AnimationClip.Loop.LOOP,
                "idle movement layer");
        request = state.update(locomotion(true,true,false,true,false,
                LocomotionAnimationBridge.Direction.FORWARD), clips, "movement_idle");
        check(request != null && "walk_forward".equals(request.clip), "walk forward");
        request = state.update(locomotion(true,true,true,true,false,
                LocomotionAnimationBridge.Direction.FORWARD), clips, "walk_forward");
        check(request != null && "run_start".equals(request.clip) && request.restart
                && request.loop == AnimationClip.Loop.ONCE, "sprint entrance");
        check(state.update(locomotion(true,true,true,true,false, LocomotionAnimationBridge.Direction.FORWARD),
                clips, "run_start") == null, "sprint entrance not restarted");
        request = state.update(locomotion(true,true,true,true,false,
                LocomotionAnimationBridge.Direction.FORWARD), clips, null);
        check(request != null && "run".equals(request.clip) && request.loop == AnimationClip.Loop.LOOP,
                "sprint loop after entrance");
        request = state.update(locomotion(true,true,true,false,false,
                LocomotionAnimationBridge.Direction.FORWARD), clips, "run");
        check(request != null && "run_hold".equals(request.clip), "airborne sprint hold");
        request = state.update(locomotion(true,true,false,true,false,
                LocomotionAnimationBridge.Direction.BACKWARD), clips, "run_hold");
        check(request != null && "run_end".equals(request.clip) && request.loop == AnimationClip.Loop.ONCE,
                "sprint exit");
        check(state.update(locomotion(true,true,false,true,false, LocomotionAnimationBridge.Direction.BACKWARD),
                clips, "run_end") == null, "sprint exit not interrupted");
        request = state.update(locomotion(true,true,false,true,false,
                LocomotionAnimationBridge.Direction.BACKWARD), clips, null);
        check(request != null && "walk_backward".equals(request.clip), "backward walk after sprint exit");
        request = state.update(locomotion(true,true,false,true,true,
                LocomotionAnimationBridge.Direction.SIDEWAY), clips, "walk_backward");
        check(request != null && "walk_aiming".equals(request.clip), "ADS walk");
        request = state.update(locomotion(false,false,false,true,false,
                LocomotionAnimationBridge.Direction.FORWARD), clips, "walk_aiming");
        check(request != null && request.stop, "non-equipped context stops locomotion");
    }

    private static LocomotionAnimationBridge.Input locomotion(boolean equipped, boolean moving, boolean sprinting,
                                                               boolean onGround, boolean aiming,
                                                               LocomotionAnimationBridge.Direction direction) {
        return new LocomotionAnimationBridge.Input(equipped,moving,sprinting,onGround,aiming,direction);
    }

    private static AnimationClip eventClip(AnimationClip.Loop loop) {
        return new AnimationClip("events", 1, loop, Collections.<String, AnimationTrack>emptyMap(),
                Arrays.asList(new AnimationEvent(0,"start",null),new AnimationEvent(.2,"a",null),
                        new AnimationEvent(.2,"b",null),new AnimationEvent(.7,"c",null),new AnimationEvent(1,"end",null)),
                0, .2, 0, true);
    }

    private static final class Sink implements AnimationPlayback.EventSink {
        final List<String> hits = new ArrayList<String>();
        @Override public void onEvent(AnimationClip clip, AnimationEvent event, long generation, long cycle) {
            hits.add(event.name + ":" + generation + ":" + cycle);
        }
    }

    private static void events() {
        Sink sink = new Sink();
        AnimationPlayback playback = new AnimationPlayback(eventClip(AnimationClip.Loop.ONCE),1,1,AnimationClip.Loop.ONCE);
        playback.advance(0,sink); playback.advance(.1,sink); playback.advance(.1,sink);
        check(sink.hits.equals(Arrays.asList("start:1:0","a:1:0","b:1:0")), "ordinary crossing plus duplicate times");
        playback.advance(0,sink); near(sink.hits.size(),3,"no duplicates");
        playback.advance(2,sink); near(sink.hits.size(),5,"stall crosses all remaining markers");
        playback.advance(2,sink); near(sink.hits.size(),5,"no repeat after end");
        Sink loopSink = new Sink();
        AnimationPlayback looping = new AnimationPlayback(eventClip(AnimationClip.Loop.LOOP),2,1,AnimationClip.Loop.LOOP);
        looping.advance(2.25,loopSink);
        near(loopSink.hits.size(),13,"multi-loop stall including zero markers");
        check(loopSink.hits.get(5).equals("start:2:1"),"wrap endpoint");
        Sink reverse = new Sink();
        new AnimationPlayback(eventClip(AnimationClip.Loop.ONCE),3,-1,AnimationClip.Loop.ONCE).advance(1,reverse);
        check(reverse.hits.equals(Arrays.asList("end:3:0","c:3:0","a:3:0","b:3:0","start:3:0")),"reverse event order");
        AnimationController controller = new AnimationController(definition(eventClip(AnimationClip.Loop.ONCE)));
        Sink restarted = new Sink(); controller.advanceTo(0,restarted);
        controller.play(AnimationController.Layer.ACTION,"events"); controller.advanceTo(.3,restarted);
        controller.play(AnimationController.Layer.ACTION,"events",true,1,null); controller.advanceTo(.3,restarted);
        check(restarted.hits.contains("start:2:0"),"restart epoch");
        controller.stop(AnimationController.Layer.ACTION); controller.advanceTo(2,restarted);
        check(!restarted.hits.contains("c:2:0"),"interrupted clip has no future markers");
        AnimationClip zero = new AnimationClip("zero",0,AnimationClip.Loop.ONCE,Collections.<String,AnimationTrack>emptyMap(),
                Collections.singletonList(new AnimationEvent(0,"zero",null)),0,0,0,true);
        AnimationController instant = new AnimationController(definition(zero)); Sink instantSink = new Sink();
        instant.play(AnimationController.Layer.ACTION,"zero"); instant.advanceTo(0,instantSink); instant.advanceTo(0,instantSink);
        near(instantSink.hits.size(),1,"zero duration marker once");
        check(!instant.active("zero"),"zero duration finishes");
    }

    private static void isolationAndClock() {
        AnimationClip clip = new AnimationClip("move",1,AnimationClip.Loop.ONCE,
                Collections.singletonMap("bolt",new AnimationTrack(Arrays.asList(key(0,0),key(1,10)))),
                eventClip(AnimationClip.Loop.ONCE).events,0,.5,0,true);
        AnimationDefinition shared = definition(clip);
        AnimationController a = new AnimationController(shared), b = new AnimationController(shared);
        a.play(AnimationController.Layer.ACTION,"move"); b.play(AnimationController.Layer.ACTION,"move");
        a.advanceTo(0,null); b.advanceTo(0,null); a.advanceTo(.5,null);
        near(a.sample(baseline(0)).get("bolt").x,5,"first instance advances");
        near(b.sample(baseline(0)).get("bolt").x,0,"second instance isolated");
        near(clip.tracks.get("bolt").keyframes.get(0).transform.x,0,"definition unchanged");
        near(a.progress(AnimationController.Layer.ACTION),.5,"normalized progress");
        for (int fps : new int[]{30,60,120,240}) {
            AnimationController timed = new AnimationController(shared); Sink sink = new Sink();
            timed.sample(baseline(0)); timed.play(AnimationController.Layer.ACTION,"move"); timed.advanceTo(0,sink);
            for (int frame=1; frame <= fps * 2; frame++) {
                double time = frame / (double)fps;
                timed.advanceTo(time,sink); timed.sample(baseline(0));
                timed.advanceTo(time,sink); // second render pass
            }
            near(sink.hits.size(),5,"events at " + fps + " FPS");
            near(timed.sample(baseline(0)).get("bolt").x,0,"completed fade at " + fps + " FPS");
        }
        AnimationController stalled = new AnimationController(shared);
        stalled.sample(baseline(0)); stalled.play(AnimationController.Layer.ACTION,"move"); stalled.advanceTo(0,null);
        stalled.advanceTo(1.25,null);
        near(stalled.sample(baseline(0)).get("bolt").x,5,"stall spends remainder in exit fade");
        expectFailure(() -> stalled.advanceTo(1,null),"backwards");
    }

    private static void reloadBridge() {
        Set<String> variants = new LinkedHashSet<String>(Arrays.asList("reload_tactical", "reload_empty", "reload"));
        List<String> requests = new ArrayList<String>();
        ReloadAnimationBridge.State tactical = new ReloadAnimationBridge.State();
        ReloadAnimationBridge.StartEvent tacticalEvent = ReloadAnimationBridge.acceptedEvent(true, 1, 2, 100, false);
        ReloadAnimationBridge.Request request = tactical.accept(tacticalEvent, 2, 100, variants);
        if (request != null) requests.add(request.layer + ":" + request.clip + ":" + request.restart);
        check(requests.equals(Collections.singletonList("ACTION:reload_tactical:true")),
                "accepted tactical reload makes one restarting ACTION request");
        check(tactical.accept(tacticalEvent, 2, 100, variants) == null, "duplicate reload event ignored");

        AnimationController tacticalController = new AnimationController(definition(
                onceClip("reload_tactical", 1), onceClip("reload_empty", 1), onceClip("reload", 1)));
        tacticalController.advanceTo(0, null);
        tactical.started(tacticalController.play(request.layer, request.clip, request.restart, 1, null));
        tacticalController.advanceTo(.5, null);
        check(tactical.ownsAction() && tactical.presentationReload(false),
                "imported reload owns renderer pose while ACTION is active");
        near(tacticalController.progress(AnimationController.Layer.ACTION), .5,
                "imported reload continues after gameplay state becomes false");
        check(tactical.presentationReload(true), "later IsReloading true does not alter reload ownership");
        check(requests.size() == 1, "IsReloading true does not request a second clip");
        tacticalController.advanceTo(1, null);
        check(tacticalController.current(AnimationController.Layer.ACTION) == null,
                "reload ACTION reaches its natural duration");
        tactical.finishNaturally();
        check(!tactical.ownsAction() && !tactical.presentationReload(false),
                "natural completion returns renderer ownership to default");

        ReloadAnimationBridge.State empty = new ReloadAnimationBridge.State();
        ReloadAnimationBridge.StartEvent emptyEvent = ReloadAnimationBridge.acceptedEvent(true, 2, 2, 100, true);
        request = empty.accept(emptyEvent, 2, 100, variants);
        check(request != null && request.layer == AnimationController.Layer.ACTION
                && "reload_empty".equals(request.clip), "accepted empty reload requests empty ACTION clip");
        check(empty.accept(emptyEvent, 2, 100, variants) == null, "accepted empty reload starts exactly once");
        empty.started(true);
        check(empty.presentationReload(false), "gameplay completion does not cancel empty reload");
        check("reload_empty".equals(empty.invalidate()), "explicit invalidation identifies cancelled ACTION clip");
        check(!empty.ownsAction(), "explicit invalidation releases ACTION owner");

        ReloadAnimationBridge.State rejected = new ReloadAnimationBridge.State();
        check(ReloadAnimationBridge.matches(new ReloadAnimationBridge.StartEvent(3, 2, 100, false), 2, 100),
                "accepted reload identity also routes to the legacy presentation timer");
        check(!ReloadAnimationBridge.matches(new ReloadAnimationBridge.StartEvent(3, 1, 100, false), 2, 100)
                        && !ReloadAnimationBridge.matches(new ReloadAnimationBridge.StartEvent(3, 2, 101, false), 2, 100),
                "legacy reload routing rejects another slot or gun");
        check(rejected.accept(ReloadAnimationBridge.acceptedEvent(false, 3, 2, 100, false),
                2, 100, variants) == null, "rejected reload emits no animation event");
        check(rejected.accept(new ReloadAnimationBridge.StartEvent(4, 1, 100, false),
                2, 100, variants) == null, "slot change does not animate another stack");
        check(rejected.accept(new ReloadAnimationBridge.StartEvent(5, 2, 101, false),
                2, 100, variants) == null, "weapon change does not animate another gun");

        ReloadAnimationBridge.State switched = new ReloadAnimationBridge.State();
        request = switched.accept(new ReloadAnimationBridge.StartEvent(6, 2, 100, false), 2, 100, variants);
        switched.started(request != null);
        check("reload_tactical".equals(switched.invalidateIfIdentityChanged(3, 100))
                && !switched.ownsAction(), "selected-slot change cancels active reload ownership");

        ReloadAnimationBridge.State legacy = new ReloadAnimationBridge.State();
        check(legacy.accept(new ReloadAnimationBridge.StartEvent(7, 2, 100, false), 2, 100,
                Collections.singleton("idle")) == null, "missing imported reload clip makes no ACTION request");
        check(legacy.presentationReload(true) && !legacy.ownsAction(),
                "authoritative reload remains available to legacy renderer");
        ReloadAnimationBridge.State fallback = new ReloadAnimationBridge.State();
        request = fallback.accept(new ReloadAnimationBridge.StartEvent(8, 2, 100, true),
                2, 100, Collections.singleton("reload"));
        check(request != null && "reload".equals(request.clip), "reload alias remains the variant fallback");
    }

    private static void legacyReloadRegression() throws Exception {
        AnimationDefinition local = definition(clip("reload",20,1,5,true));
        AnimationDefinition defaults = definition(clip("reload_empty",99,0,0,true),
                clip("reload_tactical",98,0,0,true), clip("walk_forward",2,0,0,true));
        AnimationDefinition merged = local.withFallback(defaults);
        check(!merged.clips.containsKey("reload_empty") && !merged.clips.containsKey("reload_tactical"),
                "shared reload variants cannot take selection away from local HMG reload");
        check(merged.requireClip("reload") == local.requireClip("reload"), "local reload retains ownership");
        check(merged.clips.containsKey("walk_forward") && !local.clips.containsKey("walk_forward"),
                "missing-clip merge does not mutate local source");
        check(defaults.clips.containsKey("reload_empty"), "shared source is not mutated");
        for (boolean empty : new boolean[]{false,true}) {
            ReloadAnimationBridge.Request request = new ReloadAnimationBridge.State().accept(
                    new ReloadAnimationBridge.StartEvent(1,0,10,empty),0,10,merged.clips.keySet());
            check(request != null && "reload".equals(request.clip), "original HMG reload selected for empty=" + empty);
        }
        AnimationDefinition legacyNames = parse("\"reload_dry\":{\"duration\":1}");
        check(!legacyNames.clips.containsKey("reload_empty"), "legacy HMG names are not globally aliased");

        AnimationController controller = new AnimationController(merged);
        controller.sample(baseline(0)); controller.advanceTo(0,null);
        controller.play(AnimationController.Layer.ACTION,"reload");
        controller.advanceTo(.25,null);
        near(controller.sample(baseline(0)).get("bolt").x,5,"reload fade begins normally");
        controller.play(AnimationController.Layer.MOVEMENT,"walk_forward");
        near(controller.sample(baseline(0)).get("bolt").x,5,"movement start preserves reload fade");
        controller.advanceTo(.5,null);
        controller.stop(AnimationController.Layer.MOVEMENT);
        near(controller.sample(baseline(0)).get("bolt").x,10,"movement stop preserves reload fade");

        AnimationClip sparse = new AnimationClip("reload",1,AnimationClip.Loop.HOLD,
                Collections.singletonMap("magazine",new AnimationTrack(Collections.singletonList(key(0,20)))),
                Collections.<AnimationEvent>emptyList(),0,0,5,true);
        AnimationController layers = new AnimationController(definition(sparse,clip("walk",5,0,0,true),clip("fire",2,0,0,true)));
        layers.play(AnimationController.Layer.MOVEMENT,"walk");
        layers.play(AnimationController.Layer.ACTION,"reload");
        near(layers.sample(baseline(7)).get("bolt").x,7,"sparse reload suppresses movement on omitted bones");
        layers.play(AnimationController.Layer.ADDITIVE,"fire");
        near(layers.sample(baseline(7)).get("bolt").x,9,"additive fire remains after action");

        String json = "{\"animations\":{\"reload\":{\"animation_length\":1,\"bones\":{\"root\":{\"position\":[1,2,3],\"rotation\":[10,20,30]}}}}}";
        AnimationDefinition nativeClip = new BedrockAnimationLoader(true).parse(new StringReader(json),"native");
        AnimationDefinition projectClip = new BedrockAnimationLoader().parse(new StringReader(json),"project");
        AnimationPose.Transform pose = nativeClip.requireClip("reload").tracks.get("root").sample(0);
        near(pose.x,3.0/16,"native Bedrock translation X"); near(pose.y,-6.0/16,"native Bedrock translation Y");
        near(pose.rx,10,"native Bedrock rotation X"); near(pose.ry,20,"native Bedrock rotation Y");
        near(projectClip.requireClip("reload").tracks.get("root").sample(0).x,-3.0/16,
                "existing bbmodel fallback coordinate convention unchanged");
        java.nio.file.Path temporary = java.nio.file.Files.createTempDirectory("hmg-animation-ownership-");
        try {
            File a = java.nio.file.Files.createDirectories(temporary.resolve("a")).resolve("reload.animation.json").toFile();
            File b = java.nio.file.Files.createDirectories(temporary.resolve("b")).resolve("reload.animation.json").toFile();
            java.nio.file.Files.write(a.toPath(),json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            java.nio.file.Files.write(b.toPath(),json.replace("[1,2,3]","[4,5,6]").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            BedrockAnimationLoader cache = new BedrockAnimationLoader(true);
            AnimationDefinition first = cache.load(a), second = cache.load(b);
            check(first != second,"same-named animation files in different packs have independent cache entries");
            check(cache.load(new File(a.getParentFile(),"./reload.animation.json")) == first,"canonical animation identity");
            check(BedrockAnimationLoader.retainKnownParts(first,Collections.<String>emptySet(),new HashSet<String>()).partNames.isEmpty()
                    && first.partNames.contains("root"),"per-gun track filtering cannot mutate cached animation");
        } finally {
            try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(temporary)) {
                for (java.nio.file.Path path : (Iterable<java.nio.file.Path>)paths.sorted(Comparator.reverseOrder())::iterator)
                    java.nio.file.Files.delete(path);
            }
        }
    }

    private static AnimationClip onceClip(String name, double duration) {
        return new AnimationClip(name, duration, AnimationClip.Loop.ONCE,
                Collections.<String, AnimationTrack>emptyMap(), Collections.<AnimationEvent>emptyList(),
                0, 0, 0, true);
    }

    private static AnimationKeyframe key(double time,float x) { return new AnimationKeyframe(time,new AnimationPose.Transform(x,0,0,0,0,0)); }
    private static void check(boolean value,String message) { checks++; if (!value) throw new AssertionError(message); }
    private static void near(double value,double expected,String message) { check(Math.abs(value-expected)<.00001,message+": "+value+" != "+expected); }
    private interface Checked { void run() throws Exception; }
    private static void expectFailure(Checked action,String message) {
        try { action.run(); } catch (Exception expected) {
            check(message == null || (expected.getMessage()!=null && expected.getMessage().contains(message)),"diagnostic: "+expected);
            return;
        }
        throw new AssertionError("Expected failure: "+message);
    }
}
