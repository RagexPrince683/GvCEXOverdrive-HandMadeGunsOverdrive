package handmadeguns.animation;

import java.util.Set;

/** Selects ordinary locomotion clips without importing a pack's gameplay/state-machine script. */
public final class LocomotionAnimationBridge {
    public enum Direction { FORWARD, BACKWARD, SIDEWAY }

    public static final class Input {
        public final boolean equipped, moving, sprinting, onGround, aiming;
        public final Direction direction;

        public Input(boolean equipped, boolean moving, boolean sprinting, boolean onGround,
                     boolean aiming, Direction direction) {
            this.equipped = equipped;
            this.moving = moving;
            this.sprinting = sprinting;
            this.onGround = onGround;
            this.aiming = aiming;
            this.direction = direction == null ? Direction.FORWARD : direction;
        }
    }

    public static final class Request {
        public final String clip;
        public final boolean restart, stop;
        public final AnimationClip.Loop loop;

        private Request(String clip, boolean restart, boolean stop, AnimationClip.Loop loop) {
            this.clip = clip;
            this.restart = restart;
            this.stop = stop;
            this.loop = loop;
        }
    }

    public static final class State {
        private boolean sprinting, enteringSprint, exitingSprint;
        private String transitionClip;

        public Request update(Input input, Set<String> clips, String current) {
            if (!input.equipped) {
                reset();
                return current == null ? null : stop();
            }

            boolean wantsSprint = input.sprinting && input.moving && !input.aiming;
            if (wantsSprint) {
                exitingSprint = false;
                if (!sprinting) {
                    sprinting = true;
                    transitionClip = first(clips, "run_start", "sprint_start", "sprinting_start");
                    if (transitionClip != null) {
                        enteringSprint = true;
                        return play(transitionClip, true, AnimationClip.Loop.ONCE, current);
                    }
                }
                if (enteringSprint) {
                    if (transitionClip.equals(current)) return null;
                    enteringSprint = false;
                }
                String desired = !input.onGround
                        ? first(clips, "run_hold", "sprint_hold", "sprinting_hold", "run", "sprint", "sprinting")
                        : first(clips, "run", "sprint", "sprinting");
                return select(desired, AnimationClip.Loop.LOOP, current);
            }

            if (sprinting || enteringSprint) {
                sprinting = false;
                enteringSprint = false;
                transitionClip = first(clips, "run_end", "sprint_end", "sprinting_end");
                if (transitionClip != null) {
                    exitingSprint = true;
                    return play(transitionClip, true, AnimationClip.Loop.ONCE, current);
                }
            }
            if (exitingSprint) {
                if (transitionClip.equals(current)) return null;
                exitingSprint = false;
            }

            String desired = null;
            if (input.onGround && input.moving) {
                if (input.aiming)
                    desired = first(clips, "walk_aiming", "walking_aiming", "walk_ads", "walking_ads");
                if (desired == null && input.direction == Direction.BACKWARD)
                    desired = first(clips, "walk_backward", "walking_backward");
                if (desired == null && input.direction == Direction.SIDEWAY)
                    desired = first(clips, "walk_sideway", "walk_sideways", "walking_sideway", "walking_sideways");
                if (desired == null)
                    desired = first(clips, "walk_forward", "walking_forward", "walk", "walking");
            } else {
                // Static/base idle remains active underneath this additive layer.
                desired = first(clips, "movement_idle", "locomotion_idle");
            }
            return select(desired, AnimationClip.Loop.LOOP, current);
        }

        private void reset() {
            sprinting = enteringSprint = exitingSprint = false;
            transitionClip = null;
        }

        private static Request select(String clip, AnimationClip.Loop loop, String current) {
            if (clip == null) return current == null ? null : stop();
            return play(clip, false, loop, current);
        }

        private static Request play(String clip, boolean restart, AnimationClip.Loop loop, String current) {
            if (!restart && clip.equals(current)) return null;
            return new Request(clip, restart, false, loop);
        }

        private static Request stop() {
            return new Request(null, false, true, null);
        }
    }

    private static String first(Set<String> clips, String... names) {
        for (String name : names) if (clips.contains(name)) return name;
        return null;
    }

    private LocomotionAnimationBridge() { }
}
