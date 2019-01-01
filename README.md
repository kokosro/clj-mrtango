# clj-mrtango

A Clojure library designed to connect to mistertango.com api using specifications found at [https://bank.mistertango.com/api_v1_doc]

## Usage

```[org.clojars.kokos/clj-mrtango "0.1.0"]```

###
```clojure
(ns example
(:require [clj-mrtango.core :as mrtango]))

(def conf {:url "https://api.mistertango.com:8445"
          :base "v1"
          :credentials {:key "..."
                       :secret "..."
                       :user "..."}})

(let [balance (mrtango/get-balance conf {})]
     (println balance))

```
## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
