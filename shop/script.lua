counter = 0
request = function()
	path = "/query/" .. counter
	counter = counter + 1
	return wrk.format(nil, path)
end
