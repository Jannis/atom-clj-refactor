(ns atom-clj-refactor.core
  (:require [clojure.walk :refer [keywordize-keys]]
            [atom-clj-refactor.editor :as e]))

(enable-console-print!)

;;;; Atom commands

(defn add-declarations! [editor symbols]
  (doseq [[cursor token] symbols]
    (println cursor token)
    (when token
      (let [text (str "(declare " (:value token) ")")
            tokens (e/tokenize-text editor)
            decls (e/find-blocks tokens
                                 #(e/has-value? % "declare")
                                 "meta.expression.clojure")
            ns' (first (e/find-blocks tokens
                                      #(e/has-value? % "ns")
                                      "meta.expression.clojure"))]
        (if-not (empty? decls)
          (e/insert-after-block editor (last decls) (str "\n" text))
          (e/insert-after-block editor ns' (str "\n\n" text)))))))

(defn add-declaration! []
  (println "atom-clj-refactor:add-declaration")
  (let [editor (e/active-editor)
        cursors (e/cursors editor)
        tokens (e/tokenize-text editor)]
    (->> cursors
      (map (fn [cursor]
             (e/tokens-in-scope tokens cursor
                                "meta.definition.global.clojure")))
      (map (fn [tokens]
             (e/find-token tokens
                           #(e/has-scope? % "entity.global.clojure"))))
      (zipmap cursors)
      (add-declarations! editor))))

(defn cycle-privacies! [editor symbols]
  (doseq [[cursor token] symbols]
    (println cursor token)
    (let [text ({"defn" "defn-" "defn-" "defn"} (e/value token))]
      (println "value" (e/value token) "text" text)
      (when text
        (e/replace-token editor token text)))))

(defn cycle-privacy! []
  (println "atom-clj-refactor:cycle-privacy")
  (let [editor (e/active-editor)
        cursors (e/cursors editor)
        tokens (e/tokenize-text editor)]
    (->> cursors
      (map (fn [cursor]
             (e/tokens-in-scope tokens cursor
                                "meta.definition.global.clojure")))
      (map (fn [tokens]
             (e/find-token tokens
                           #(and (e/has-scope? % "keyword.control.clojure")
                                 (e/value-in? % ["defn" "defn-"])))))
      (zipmap cursors)
      (cycle-privacies! editor))))

;;;; Package lifecycle

(defn activate [state]
  (println "atom-clj-refactor activated")
  (js/atom.commands.add "atom-workspace"
    (js-obj "atom-clj-refactor:add-declaration" add-declaration!
            "atom-clj-refactor:cycle-privacy" cycle-privacy!)))

(defn deactivate []
  (println "atom-clj-refactor deactivated"))

(set! js/module.exports
  (js-obj "activate" activate
          "deactivate" deactivate
          "serialize" (constantly nil)))

(set! *main-cli-fn* (constantly nil))
