(ns {{name}}.events.cdek-search-city
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]))

(defn normalize-body [response]
  (or (:body response) (get response "body") response))

(defn error-body [body]
  (when (map? body)
    (let [status (or (:status body) (get body "status"))
          error (or (:error body) (get body "error"))
          errors (or (:errors body) (get body "errors"))]
      (when (or error errors (= "error" status))
        body))))

(defn extract-cities [body]
  (cond
    (sequential? body) body
    (map? body) (or (:cities body)
                    (get body "cities")
                    [])
    :else []))

(defn cdek_search_city
  ([search-text on-success]
   (cdek_search_city search-text on-success nil))
  ([search-text on-success on-error]
   (let [value (or search-text "")]
     (if (< (count value) 2)
       (when on-success (on-success []))
       (http/ajax-request-with-headers
        {:uri (api_uri_maker "cdek-search-city")
         :method :post
         :params {:search_text value}
         :handler (fn [[ok? response]]
                    (let [body (normalize-body response)]
                      (if ok?
                        (if-let [err (error-body body)]
                          (when on-error (on-error err))
                          (when on-success (on-success (extract-cities body))))
                        (when on-error (on-error body)))))
         :format (ajax/json-request-format)
         :response-format (ajax/json-response-format {:keywords? true})})))))
