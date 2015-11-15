(ns atom-clj-refactor.core
  (:require [clojure.walk :refer [keywordize-keys]]
            [atom-clj-refactor.tokens :as t]))

(enable-console-print!)

;;;; Atom interop

(defn point->vec [p]
  (vector (.-row p) (.-column p)))

(defn ->range [v]
  (clj->js v))

(defn active-editor []
  (js/atom.workspace.getActiveTextEditor))

(defn cursor-positions [editor]
  (into []
        (map point->vec)
        (array-seq (.getCursorBufferPositions editor))))

;;;; Clojure parsing

(defn top-level-symbol [editor position]
  (let [grammar (.getGrammar editor)
        range (->range (vector [0 0] position))
        tokens (->> (.getTextInBufferRange editor range)
                    (.tokenizeLines grammar)
                    js->clj
                    flatten
                    (map keywordize-keys))]
    (t/find-global-def tokens)))

(defn print-top-level-map [m]
  (doseq [[pos global-def] m]
    (println "pos" pos)
    (println "  global-def" global-def)))

;;;; Commands

(defn add-declarations! [editor decls]
  (doseq [[pos token] decls]
    (println pos token)
    (when token
      (let [declaration (str "(declare " (:value token) ")")]
        (.. editor (insertText declaration))))))

(defn add-declaration! []
  (println "atom-clj-refactor:add-declaration")
  (let [editor (active-editor)
        positions (cursor-positions editor)]
    (->> positions
         (map #(top-level-symbol editor %))
         (map js->clj)
         (zipmap positions)
         (add-declarations! editor))))

;;;; Package lifecycle

(defn activate [state]
  (println "atom-clj-refactor activated")
  (js/atom.commands.add "atom-workspace"
    (js-obj "atom-clj-refactor:add-declaration" add-declaration!)))

(defn deactivate []
  (println "atom-clj-refactor deactivated"))

(set! js/module.exports
  (js-obj "activate" activate
          "deactivate" deactivate
          "serialize" (constantly nil)))

(set! *main-cli-fn* (constantly nil))
