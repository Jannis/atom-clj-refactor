(ns atom-clj-refactor.tokens
  (:require [clojure.string :as str]))

(defn globally-defined-entity? [t]
  (= (vector "source.clojure"
             "meta.expression.clojure"
             "meta.definition.global.clojure"
             "entity.global.clojure")
     (get t "scopes")))

(defn parent-scope? [s1 s2]
  (= (take (count s1) s1)
     (take (count s1) s2)))

(defn sibling-scope? [s1 s2]
  (= (butlast s1) (butlast s2)))

(defn whitespace-token? [t]
  (and (= "" (str/trim (:value t)))
       (not (= "invalid.trailing-whitespace"
               (last (get t :scopes))))))

(defn parent-token? [t1 t2]
  {:pre [(not-any? whitespace-token? [t1 t2])]}
  (parent-scope? (get t1 :scopes) (get t2 :scopes)))

(defn sibling-token? [t1 t2]
  {:pre [(not-any? whitespace-token? [t1 t2])]}
  (sibling-scope? (get t1 :scopes) (get t2 :scopes)))

(defn global-def-token? [t]
  (= "entity.global.clojure" (last (get t :scopes))))

(defn token-depth [t]
  (count (get t :scopes)))

(defn find-global-def [tokens]
  (let [tokens' (->> tokens
                     (filter (complement whitespace-token?))
                     reverse)]
    (loop [t (first tokens') ts (rest tokens')]
      (if (global-def-token? t)
        t
        (when-not (empty? ts)
          (let [remaining (drop-while #(< (token-depth t) (token-depth %)) ts)]
            (recur (first remaining) (rest remaining))))))))
