;; --------------------------------------------------------------------------------
(ns ld48.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]))

(declare ld48 title-screen main-screen)
(def speed 0.25)
(def sub-speed 2.5)
(def torpedo-speed 10.0)

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

(defn check-game-over [entities]
  (let [target        (first (filter :target? entities))
        target-x      (:x target)
        target-damage (:damage target)
        submarine-x   (:x (first (filter :submarine? entities)))]
    (when (> (+ submarine-x 100) target-x)
      (if (> target-damage 3)
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
       (map move-background)))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (let [background (assoc (texture "MainBackground.png")
                       :x 0 :width (* 4 1280) :background? true)
          submarine (assoc (texture "Submarine.png")
                      :x 20 :y (/ 720 2) :submarine? true)
          torpedo (assoc (texture "torpedo.png")
                    :x -256 :y 100 :torpedo? true :count 5 :active? false)
          target (assoc (texture "target.png")
                   :x (- (* 1 1280) 256) :y (- 720 128) :target? true :damage 0)] ;; FIXME
      [background submarine torpedo target]))

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

  ;; restart the game
  (app! :post-runnable #(set-screen! ld48 title-screen))

  current-keycodes
  )
