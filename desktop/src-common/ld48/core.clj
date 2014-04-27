;; --------------------------------------------------------------------------------
(ns ld48.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]))

(declare ld48 title-screen main-screen)
(def speed 0.25)
(def sub-speed 5.0)

;; --------------------------------------------------------------------------------
(defscreen title-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (let [background (texture "TitleBackground.png")
          title-text (assoc (texture "TitleText.png")
                       :x (/ (- 1280 640) 2), :y (/ (- 720 480) 2))]
      [background title-text]))

  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities))

  :on-key-down
  (fn [screen entities]
    (set-screen! ld48 main-screen)))

;; --------------------------------------------------------------------------------
(defn is-pressed? [screen x]
  (= (:keycode screen) (key-code x)))

(defn- get-direction [screen]
  ;;(println (:keycode screen) (key-code :dpad-up) (key-code :dpad-down))
  (cond
   (is-pressed? screen :dpad-up) :up
   (is-pressed? screen :dpad-down) :down))

(defn move-background [{:keys [background?] :as entity}]
  (if background?
    (assoc entity :x (- (:x entity) speed))
    entity))

(defn- update-submarine-position [direction {:keys [submarine?] :as entity}]
  (if submarine?
    (let [old-y (:y entity)
          new-y (case direction
                  :up (+ old-y sub-speed)
                  :down (- old-y sub-speed))
          new-y (min (max new-y 0) 720)]
      (assoc entity :y new-y :direction direction))
    entity))

(defn- move-player [direction entities]
  (map #(update-submarine-position direction %) entities))

(defn per-render-update [entities]
  (map move-background entities))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (let [background (assoc (texture "MainBackground.png")
                       :x 0 :width (* 4 1280) :background? true)
          submarine (assoc (texture "Submarine.png")
                      :x 20 :y (/ 720 2) :submarine? true)]
      [background submarine]))

  :on-render
  (fn [screen entities]
    (let [updated-entities (per-render-update entities)]
      (clear!)
      (render! screen updated-entities)))

  :on-key-down
  (fn [screen entities]
    (let [direction (get-direction screen)]
      (println "direction=" direction)
      (cond
       (is-pressed? screen :r) (app! :post-runnable #(set-screen! ld48 main-screen))
       direction (move-player direction entities)
       :else entities))))

;; --------------------------------------------------------------------------------
;; from tutorial for error handling
(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear! 1.0 0.0 0.0 1.0)))

;; --------------------------------------------------------------------------------
(defgame ld48
  :on-create
  (fn [this]
    (set-screen! this title-screen)))

;; --------------------------------------------------------------------------------
(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                         (catch Exception e
                           (.printStackTrace e)
                           (set-screen! ld48 blank-screen)))))

;; --------------------------------------------------------------------------------
;; here are repl helpers
(comment
  ;; instarepl this code & desktop_launcher, make instarepl and
  ;; uncomment & exec the following code.  Don't exec this here.
  ;(use 'ld48.core.desktop-launcher)
  ;(-main)

  ;; look at the main game state: select code & cmd-enter
  (-> title-screen :entities deref)
  (-> title-screen :screen deref)
  (-> main-screen :entities deref)
  (-> main-screen :screen deref)

  ;; restart the game
  (app! :post-runnable #(set-screen! ld48 title-screen))

  )
