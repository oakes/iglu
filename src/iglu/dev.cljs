(ns iglu.dev
  (:require iglu.examples
            iglu.examples-2d
            iglu.examples-3d
            iglu.examples-advanced
            dynadoc.core
            [orchestra-cljs.spec.test :as st]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as s]))

(st/instrument)
(set! s/*explain-out* expound/printer)
