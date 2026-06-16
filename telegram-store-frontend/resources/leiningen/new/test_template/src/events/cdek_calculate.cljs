(ns {{name}}.events.cdek-calculate
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.apiurimaker :refer [api_uri_maker]]))

(defn cdek_calculate
  ([params on-success]
   (cdek_calculate params on-success nil))
  ([params on-success on-error]
   (http/ajax-request-with-headers
    {:uri (api_uri_maker "cdek-calculate")
     :method :post
     :params params
     :handler (fn [[ok? response]]
                (if ok?
                  (when on-success (on-success response))
                  (when on-error (on-error response))))
     :format (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})})))
