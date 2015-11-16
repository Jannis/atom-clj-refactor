(ns atom-clj-refactor.core
  (:require [clojure.walk :refer [keywordize-keys]]
            [atom-clj-refactor.editor :as e]
            [atom-clj-refactor.lang :as lang]))

(enable-console-print!)

;;;; Atom commands

(defn add-declarations! [editor symbols]
  (doseq [[cursor token] symbols]
    (when token
      (let [text (str "(declare " (e/value token) ")")
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
      (map #(e/get-scope tokens % "meta.definition.global.clojure"))
      (map (fn [tokens]
             (e/find-first tokens #(e/has-scope? % "entity.global.clojure"))))
      (zipmap cursors)
      (add-declarations! editor))))

(defn cycle-def-privacy! [editor tokens]
  (let [simple-meta (e/find-scope tokens "meta.metadata.simple.clojure")
        map-meta (e/find-scope tokens "meta.metadata.map.clojure")
        all-meta (merge (lang/read-meta map-meta)
                        (lang/read-meta simple-meta))
        new-meta (if (:private all-meta)
                   (dissoc all-meta :private)
                   (assoc all-meta :private true))
        new-meta-text (lang/write-meta new-meta)]
    (cond
      (not (empty? simple-meta))
        (e/replace-tokens editor simple-meta new-meta-text)
      (not (empty? map-meta))
        (e/replace-tokens editor map-meta new-meta-text)
      :else
        (when-not (empty? new-meta)
          (e/insert-after-token editor (first tokens)
                                (str " " new-meta-text))))))

(defn cycle-privacies! [editor symbols]
  (doseq [[cursor tokens] symbols]
    (when-not (empty? tokens)
      (case (e/value (first tokens))
        "def" (cycle-def-privacy! editor tokens)
        "defn" (e/replace-token editor (first tokens) "defn-")
        "defn-" (e/replace-token editor (first tokens) "defn")))))

(defn cycle-privacy! []
  (println "atom-clj-refactor:cycle-privacy")
  (let [editor (e/active-editor)
        cursors (e/cursors editor)
        tokens (e/tokenize-text editor)]
    (->> cursors
      (map #(e/get-scope tokens % "meta.definition.global.clojure"))
      (map (fn [tokens]
             (e/find-token #(and (e/value-in? % ["def" "defn" "defn-"])
                                 (e/has-scope? % "keyword.control.clojure"))
                           tokens)))
      (zipmap cursors)
      (cycle-privacies! editor))))

;;;; Package lifecycle

(defn activate [state]
  (println "atom-clj-refactor activated")
  (js/atom.commands.add "atom-workspace"
    (js-obj "atom-clj-refactor:add-declaration" add-declaration!
            "atom-clj-refactor:cycle-privacy" cycle-privacy!)))

(set! js/module.exports
  (js-obj "activate" activate
          "deactivate" (constantly nil)
          "serialize" (constantly nil)))

(set! *main-cli-fn* (constantly nil))
