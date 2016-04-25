# init replica
rs.initiate()
rs.conf()   // verify

# add replica
rs.add("slave name")
rs.status()