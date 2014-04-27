(ns ld48.core.desktop-launcher
  (:require [ld48.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. ld48 "ld48" 1280 720)
  (Keyboard/enableRepeatEvents true))
