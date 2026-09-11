package handmadeguns.animation;

import handmadeguns.client.modelLoader.blockbench.BlockbenchProject;
import java.io.*;
import java.util.*;

/** Dependency-free regression entry point: Gradle animationTest, or Java 8 + Minecraft's Gson. */
public final class AnimationTests {
    private static int checks;
    private static final AnimationLoader LOADER = new AnimationLoader();

    public static void main(String[] args) throws Exception {
        parsing(); evaluation(); transitions(); events(); isolationAndClock(); reloadBridge();
        if (args.length > 0) {
            AnimationDefinition example = LOADER.load(new File(args[0]));
            example.validateParts(new LinkedHashSet<String>(Arrays.asList("bolt", "magazine")));
            for (String clip : Arrays.asList("idle", "fire", "reload", "inspect")) check(example.clips.containsKey(clip), "example " + clip);
            check(LOADER.load(new File(args[0])) == example, "definition cache identity");
            LOADER.invalidate(new File(args[0]));
            check(LOADER.load(new File(args[0])) != example, "cache invalidation");
        }
        if (args.length > 1) blockbenchReload(new File(args[1]));
        System.out.println("HMG animation: " + checks + " checks passed (parser, evaluator, controller, events, isolation, reload bridge).");
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
