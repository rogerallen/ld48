(ns ld48.core
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]))

(defscreen title-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (label "ld48 game by Roger Allen" (color :white)))
  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities)))

;; from tutorial for error handling
(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!)))

(defgame ld48
  :on-create
  (fn [this]
    (set-screen! this title-screen)))

(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                         (catch Exception e
                           (.printStackTrace e)
                           (set-screen! ld48 blank-screen)))))


;; here are repl helpers
(comment
  ;; instarepl this code & desktop_launcher, make instarepl and
  ;; uncomment & exec the following code.  Don't exec this here.
  ;(use 'ld48.core.desktop-launcher)
  ;(-main)

  ;; look at the main game state: select code & cmd-enter
  (-> title-screen :entities deref)

  ;; restart the game
  (app! :post-runnable #(set-screen! ld48 title-screen))

  )
