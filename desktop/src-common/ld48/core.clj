;; --------------------------------------------------------------------------------
(ns ld48.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.math :refer :all]))

(declare ld48 title-screen main-screen)
(def speed 0.25)
(def sub-speed 2.5)
(def torpedo-speed 10.0)
(def mine-speed 3.0)
(def max-target-damage 3)
(def max-submarine-damage 2)

(defonce game-state (atom :startup))

;; --------------------------------------------------------------------------------
(defscreen title-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (let [background (texture "TitleBackground.png")
          title-text (assoc (texture "TitleText.png")
                       :x (/ (- 1280 640) 2), :y (/ (- 720 480) 2))
          win-text (assoc (texture "WinText.png")
                     :x (/ (- 1280 640) 2), :y (/ (- 720 480) 2))
          lose-text (assoc (texture "LoseText.png")
                      :x (/ (- 1280 640) 2), :y (/ (- 720 480) 2))
          the-text (cond
                    (= @game-state :startup) title-text
                    (= @game-state :win) win-text
                    (= @game-state :lose) lose-text)
          _ (reset! game-state :startup)
          ]
      [background the-text]))

  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities))

  :on-key-down
  (fn [screen entities]
    (set-screen! ld48 main-screen)))

;; --------------------------------------------------------------------------------
(defn get-direction []
  (cond
   (key-pressed? :dpad-up) :up
   (key-pressed? :dpad-down) :down))

(defn set-torpedo-y [y {:keys [torpedo? active?] :as entity}]
  (if torpedo?
    (assoc entity :y (+ y 16))
    entity))

(defn adjust-torpedo-y [entities]
  (let [submarine (first (filter :submarine? entities))
        torpedo (first (filter :torpedo? entities))]
    (if (not (:active? torpedo))
      (map #(set-torpedo-y (:y submarine) %) entities)
      entities)))

(defn launch-torpedo? [torpedo]
  (and (> (:count torpedo) 0)
       (not (:active? torpedo))
       (key-pressed? :dpad-right)))

(defn move-torpedo [{:keys [torpedo? active?] :as entity}]
  (if torpedo?
    (if active?
      (let [old-x         (:x entity)
            new-x         (+ old-x torpedo-speed)
            still-active? (< new-x 1280)
            new-count     (dec (:count entity))]
        (if still-active?
          (assoc entity :x new-x)
          (assoc entity :x -128 :active? false :count new-count)))
      (if (launch-torpedo? entity)
        (assoc entity :x 128 :active? true)
        entity))
    entity))

(defn move-mine [{:keys [mine? active?] :as entity}]
  (if mine?
    (if active?
      (let [new-x (- (:x entity) mine-speed)
            new-y (- (:y entity) mine-speed)
            still-active? (and (> new-x -16) (> new-y -16))]
        (if still-active?
          (assoc entity :x new-x :y new-y)
          (assoc entity :x -128 :y -128 :active? false)))
      entity)
    entity))

(defn spawn-mine [{:keys [mine? active?] :as entity}]
  (if (and mine? (not active?))
    (assoc entity :x (+ 100 (rand-int 400)) :y (- 720 16) :active? true)
    entity))

(defn move-submarine [{:keys [submarine?] :as entity}]
  (if submarine?
    (if-let [direction (get-direction)]
      (let [old-y (:y entity)
            new-y (case direction
                    :up (+ old-y sub-speed)
                    :down (- old-y sub-speed))
            new-y (min (max new-y 0) (- 720 128))]
        (assoc entity :y new-y))
      entity)
    entity))

(defn move-background [{:keys [background? target?] :as entity}]
  (if (or background? target?)
    (assoc entity :x (- (:x entity) speed))
    entity))

(defn update-hit-box [{:keys [submarine? torpedo? mine? target?] :as entity}]
  (if (or submarine? torpedo? mine? target?)
    (assoc entity :hit-box (rectangle (:x entity) (:y entity) (:width entity) (:height entity)))
    entity))

(defn set-target-damage [{:keys [torpedo? target? damage] :as entity}]
  (if target?
    (assoc entity :damage (inc damage))
    (if torpedo?
      (let [new-count (dec (:count entity))]
        (assoc entity :x -128 :active? false :count new-count))
      entity)))

(defn torpedo-target-collision [torpedo target entities]
  (let [new-damage (inc (:damage target))]
    (if (<= new-damage max-target-damage)
      (map set-target-damage entities)
      (remove (set target) entities))))

(defn set-submarine-damage [{:keys [mine? submarine? damage] :as entity}]
  (if submarine?
    (assoc entity :damage (inc damage))
    (if mine?
      (assoc entity :x -128 :y -128 :active? false)
      entity)))

(defn mine-submarine-collision [mine submarine entities]
  (let [new-damage (inc (:damage submarine))]
    (if (<= new-damage max-submarine-damage)
      (map set-submarine-damage entities)
      (remove (set submarine) entities))))

(defn adjust-damage [entities]
  (let [torpedo (first (filter :torpedo? entities))
        target (first (filter :target? entities))
        mine (first (filter :mine? entities))
        submarine (first (filter :submarine? entities))
        torpedo-touched-target (rectangle! (:hit-box torpedo) :overlaps (:hit-box target))
        mine-touched-submarine (rectangle! (:hit-box mine) :overlaps (:hit-box submarine))]
    (if torpedo-touched-target
      (torpedo-target-collision torpedo target entities)
      (if mine-touched-submarine
        (mine-submarine-collision mine submarine entities)
        entities))))

(defn check-game-over [entities]
  (let [target        (first (filter :target? entities))
        target-x      (:x target)
        target-damage (:damage target)
        submarine     (first (filter :submarine? entities))
        submarine-damage (:damage submarine)
        submarine-x   (:x submarine)]
    (when (>= submarine-damage max-submarine-damage)
      (reset! game-state :lose)
      (app! :post-runnable #(set-screen! ld48 title-screen)))
    (when (>= target-damage max-target-damage)
      (reset! game-state :win)
      (app! :post-runnable #(set-screen! ld48 title-screen)))
    (when (> (+ submarine-x 100) target-x)
        (if (> target-damage max-target-damage)
          (reset! game-state :win)
          (reset! game-state :lose))
        (app! :post-runnable #(set-screen! ld48 title-screen)))))

(defn per-render-update [entities]
  ;; Game Over?
  (check-game-over entities)
  ;; Game On...
  (->> entities
       (map move-submarine)
       (adjust-torpedo-y)
       (map move-torpedo)
       (map move-mine)
       (map move-background)
       (map update-hit-box)
       (adjust-damage)))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (add-timer! screen :spawn-mine 1 2)
    (let [background (assoc (texture "MainBackground.png")
                       :x 0 :width (* 4 1280) :background? true)
          submarine (assoc (texture "Submarine.png")
                      :x 20 :y (/ 720 2) :width 128 :height 64 :submarine? true :damage 0)
          torpedo (assoc (texture "torpedo.png")
                    :x -256 :y 100 :width 64 :height 16 :torpedo? true :count 5 :active? false)
          mine (assoc (texture "mine.png")
                    :x -100 :y -100 :width 16 :height 16 :mine? true :active? false)
          target (assoc (texture "target.png")
                   :x (- (* 1 1280) 256) :y (- 720 128) :width 256 :height 128 :target? true :damage 0)] ;; FIXME
      [background submarine torpedo mine target]))

  :on-render
  (fn [screen entities]
    (let [updated-entities (per-render-update entities)]
      (clear!)
      (render! screen updated-entities)))

  :on-key-down
  (fn [screen entities]
    (if (key-pressed? :r)
      (app! :post-runnable #(set-screen! ld48 main-screen)))
    entities)

  :on-timer
  (fn [screen entities]
    (case (:id screen)
      :spawn-mine (map spawn-mine entities)))

  )

;; --------------------------------------------------------------------------------
;; from tutorial for error handling
(defscreen error-screen
  :on-render
  (fn [screen entities]
    (clear! 1.0 0.0 0.0 1.0)))

(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                         (catch Exception e
                           (.printStackTrace e)
                           (set-screen! ld48 error-screen)))))

;; --------------------------------------------------------------------------------
(defgame ld48
  :on-create
  (fn [this]
    (set-screen! this title-screen)))

;; --------------------------------------------------------------------------------
;; here are repl helpers
(comment
  ;; instarepl this code & desktop_launcher, make instarepl and
  ;; uncomment & exec the following code.  Don't exec this here.
  (use 'ld48.core.desktop-launcher)
  ;(-main)

  ;; look at the main game state: select code & cmd-enter
  (-> title-screen :screen deref)
  (-> title-screen :entities deref)
  (-> main-screen :screen deref)
  (-> main-screen :entities deref)

  (let [hb1 (:hit-box (first (filter :torpedo? (-> main-screen :entities deref))))
        hb2 (:hit-box (first (filter :target? (-> main-screen :entities deref))))
        hb1 (rectangle 0 0 10 10)
        hb2 (rectangle 5 5 10 10)]
    (rectangle! hb1 :overlaps hb2))

  ;; restart the game
  (println "----------- restarting game -------------------")
  (app! :post-runnable #(set-screen! ld48 title-screen))

  )
