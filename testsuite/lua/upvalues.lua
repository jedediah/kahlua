
do
   do
      local s = 1
      function f()
	 return s
      end
      
      function g(x)
	 s = x
      end
   end

   testAssert(f() == 1)   
   g(10)
   testAssert(f() == 10)
end

do
   local s = 1
   function f()
      return s
   end
   
   function g(x)
      s = x
   end
   
   testAssert(f() == 1)   
   g(10)
   testAssert(f() == 10)
end

do
   local s = 1
   function f()
      return function()
		return s
	     end
   end
   
   function g(x)
      local function g2()
	 s = x
      end
      return g2
   end

   local v = f()()
   testAssert(v == 1)
   local g2 = g(10)
   g2()
   v = f()()
   testAssert(v == 10)
end

